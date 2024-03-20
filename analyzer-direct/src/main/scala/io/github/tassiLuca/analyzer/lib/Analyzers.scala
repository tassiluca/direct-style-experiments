package io.github.tassiLuca.analyzer.lib

import gears.async.Future.Collector
import gears.async.{Async, AsyncOperations, Future, Task}
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import io.github.tassiLuca.dse.boundaries.EitherConversions.given
import io.github.tassiLuca.dse.boundaries.{CanFail, either}
import io.github.tassiLuca.dse.boundaries.either.?
import io.github.tassiLuca.dse.pimping.TerminableChannelOps.foreach
import io.github.tassiLuca.dse.pimping.asTry
import io.github.tassiLuca.dse.pimping.FlowOps.{map, toSeq}

abstract class AbstractAnalyzer(repositoryService: RepositoryService) extends Analyzer:
  extension (r: Repository)
    protected def performAnalysis(using Async): Task[RepositoryReport] = Task:
      Async.group:
        val contributions = Future:
          either(repositoryService.contributorsOf(r.organization, r.name))
        val release = Future:
          either(repositoryService.lastReleaseOf(r.organization, r.name))
        RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq.empty), release.await.toOption)

private class BasicAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations, CanFail): Seq[RepositoryReport] = Async.group:
    val reposInfo = repositoryService.repositoriesOf(organizationName).map(_.performAnalysis.start())
    val collector = Collector(reposInfo.toList*)
    reposInfo.foreach: _ =>
      updateResults(collector.results.read().asTry.?.awaitResult.?)
    reposInfo.awaitAll

private class IncrementalAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations, CanFail): Seq[RepositoryReport] = Async.group:
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
  )(using Async, AsyncOperations, CanFail): Seq[RepositoryReport] = Async.group:
    repositoryService.flowingRepositoriesOf(organizationName).map: repository =>
      Future:
        val report = repository.performAnalysis.start().awaitResult.?
        synchronized(updateResults(report))
        report
    .toSeq.?.awaitAllOrCancel
