package io.github.tassiLuca.analyzer.lib

import gears.async.Future.Collector
import gears.async.{Async, AsyncOperations, Future, Task}
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import io.github.tassiLuca.dse.boundaries.EitherConversions.given
import io.github.tassiLuca.dse.boundaries.either
import io.github.tassiLuca.dse.boundaries.either.?
import io.github.tassiLuca.dse.pimping.TerminableChannelOps.foreach
import io.github.tassiLuca.dse.pimping.asTry
import io.github.tassiLuca.dse.pimping.FlowOps.{map, toSeq}

import scala.util.boundary.Label

abstract class AbstractAnalyzer(repositoryService: RepositoryService) extends Analyzer:

  extension (r: Repository)
    protected def performAnalysis(using Async): Task[RepositoryReport] = Task:
      Async.group:
        val contributions = Future:
          repositoryService.contributorsOf(r.organization, r.name)
        val release = repositoryService.lastReleaseOf(r.organization, r.name)
        RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.toOption)

private class BasicAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]] = either:
    Async.group:
      val reposInfo = repositoryService.repositoriesOf(organizationName).?
        .map(_.performAnalysis.start())
      val collector = Collector(reposInfo.toList*)
      reposInfo.foreach: _ =>
        updateResults(collector.results.read().asTry.?.awaitResult.?)
      reposInfo.awaitAll

private class IncrementalAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]] = either:
    Async.group:
      val reposInfo = repositoryService.incrementalRepositoriesOf(organizationName)
      var futureResults = Seq[Future[RepositoryReport]]()
      reposInfo.foreach: repository =>
        futureResults = futureResults :+ Future:
          val report = repository.?.performAnalysis.start().awaitResult.?
          synchronized(updateResults(report))
          report
      futureResults.awaitAllOrCancel

private class FlowingAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]] = either:
    Async.group:
      repositoryService.flowingRepositoriesOf(organizationName).map: repository =>
        Future:
          val report = repository.performAnalysis.start().awaitResult.?
          synchronized(updateResults(report))
          report
      .toSeq.?.awaitAllOrCancel
