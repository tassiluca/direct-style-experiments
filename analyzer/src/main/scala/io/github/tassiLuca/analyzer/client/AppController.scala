package io.github.tassiLuca.analyzer.client

import gears.async.{Async, Future, ReadableChannel}
import io.github.tassiLuca.analyzer.core.{Analyzer, ReportChannel, RepositoryReport}

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
      val f = Future:
        analyzer.analyze(organizationName) match
          case Right(value) => readReports(value._1, value._2)
          case Left(errorMessage) => view.error(errorMessage)
      currentComputation = Some(f)

    private def readReports(reportsChannel: ReadableChannel[Future[RepositoryReport]], expectedReports: Int): Unit =
      var organizationReport: OrganizationReport = (Map(), Set())
      for _ <- 0 until expectedReports do
        val report = reportsChannel.read().toOption.get.await
        organizationReport = (organizationReport._1.aggregatedTo(report), organizationReport._2 + report)
        view.update(organizationReport)
      view.endComputation()

    extension (m: Map[String, Long])
      private def aggregatedTo(report: RepositoryReport): Map[String, Long] =
        m ++ report.contributions.map(c => c.user -> (m.getOrElse(c.user, 0L) + c.contributions))
