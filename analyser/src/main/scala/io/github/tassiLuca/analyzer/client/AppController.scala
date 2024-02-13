package io.github.tassiLuca.analyzer.client

import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future, Task}
import io.github.tassiLuca.analyzer.core.{Analyzer, RepositoryReport}

type OrganizationReport = (Map[String, Long], Set[RepositoryReport])

trait AppController:
  def runSession(organizationName: String): Unit
  def stopSession(): Unit

object AppController:
  def apply()(using Async): AppController = DirectAppController()

  private class DirectAppController(using Async) extends AppController:
    private val view = AnalyzerView.gui(this)
    private val analyzer = Analyzer
    private var currentComputation: Option[Future[Unit]] = None

    view.run()

    override def stopSession(): Unit = currentComputation.foreach(_.cancel())

    override def runSession(organizationName: String): Unit =
      currentComputation = Some:
        Future:
          var organizationReport: OrganizationReport = (Map(), Set())
          val (expectedResults, resultChannel) = analyzer.analyze(organizationName)
          for _ <- 0 until expectedResults do
            val report = resultChannel.read().toOption.get.await
            organizationReport = (organizationReport._1.aggregatedTo(report), organizationReport._2 + report)
            view.update(organizationReport)

    extension (m: Map[String, Long])
      private def aggregatedTo(report: RepositoryReport): Map[String, Long] =
        m ++ report.contributions.map(c => c.user -> (m.getOrElse(c.user, 0L) + c.contributions))