package io.github.tassiLuca.analyzer.client

import gears.async.{Async, AsyncOperations, Task}
import io.github.tassiLuca.analyzer.core.{Analyzer, RepositoryReport}

type OrganizationReport = (Map[String, Long], Set[RepositoryReport])

trait AppController:
  def runSession(organizationName: String)(using Async, AsyncOperations): Unit
  def stopSession(): Unit

object AppController:
  def apply(): AppController = DirectAppController()

  private class DirectAppController extends AppController:
    private val view = AnalyzerView.gui(this)
    private val analyzer = Analyzer

    view.run()

    override def stopSession(): Unit = ???

    override def runSession(organizationName: String)(using async: Async, asyncOperations: AsyncOperations): Unit =
      var organizationReport: OrganizationReport = (Map(), Set())
      val (expectedResults, resultChannel) = analyzer.analyze(organizationName)
      Task {
        for _ <- 0 until expectedResults do
          val report = resultChannel.read().toOption.get.await
          organizationReport = (organizationReport._1.aggregatedTo(report), organizationReport._2 + report)
          view.update(organizationReport)
      }.run.await

    extension (m: Map[String, Long])
      private def aggregatedTo(report: RepositoryReport): Map[String, Long] =
        m ++ report.contributions.map(c => c.user -> (m.getOrElse(c.user, 0L) + c.contributions))
