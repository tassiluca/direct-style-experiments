package io.github.tassiLuca.analyzer.client

import gears.async.{Async, Future}
import io.github.tassiLuca.analyzer.core.RepositoryReport
import io.github.tassiLuca.analyzer.core.direct.Analyzer

type OrganizationReport = (Map[String, Long], Set[RepositoryReport])

trait AppController:
  def runSession(organizationName: String): Unit
  def stopSession(): Unit

object AppController:
  def apply()(using Async): AppController = DirectAppController()

  private class DirectAppController(using Async) extends AppController:
    private val view = AnalyzerView.gui(this)
    private val analyzer = Analyzer.ofGitHub
    private var currentComputation: Option[Future[Unit]] = None

    view.run()

    override def stopSession(): Unit = currentComputation.foreach(_.cancel())

    override def runSession(organizationName: String): Unit =
      var organizationReport: OrganizationReport = (Map(), Set())
      val f = Future:
        analyzer.analyze(organizationName) { report =>
          organizationReport = (organizationReport._1.aggregatedTo(report), organizationReport._2 + report)
          view.update(organizationReport)
        } match { case Left(e) => view.error(e); case Right(_) => view.endComputation() }
      currentComputation = Some(f)

    extension (m: Map[String, Long])
      private def aggregatedTo(report: RepositoryReport): Map[String, Long] =
        m ++ report.contributions.map(c => c.user -> (m.getOrElse(c.user, 0L) + c.contributions))
