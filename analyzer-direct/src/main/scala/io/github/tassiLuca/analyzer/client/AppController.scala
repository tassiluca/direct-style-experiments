package io.github.tassiLuca.analyzer.client

import gears.async.{Async, AsyncOperations, Future}
import io.github.tassiLuca.analyzer.commons.client.{AnalyzerView, AppController, OrganizationReport}
import io.github.tassiLuca.analyzer.commons.lib.RepositoryReport
import io.github.tassiLuca.analyzer.lib.{Analyzer, RepositoryService}
import io.github.tassiLuca.dse.boundaries.either

object AppController:
  def direct(using Async.Spawn, AsyncOperations): AppController = DirectAppController()

  private class DirectAppController(using Async.Spawn, AsyncOperations) extends AppController:
    private val view = AnalyzerView.gui(this)
    private val analyzer = Analyzer.incremental(RepositoryService.ofGitHub())
    private var currentComputation: Option[Future[Unit]] = None

    view.run()

    override def runSession(organizationName: String): Unit =
      var organizationReport: OrganizationReport = (Map(), Set())
      val f = Future:
        either:
          analyzer.analyze(organizationName): report =>
            organizationReport = organizationReport.mergeWith(report)
            view.update(organizationReport)
        .fold(e => view.error(e), _ => view.endComputation())
      currentComputation = Some(f)

    override def stopSession(): Unit = currentComputation.foreach(_.cancel())
