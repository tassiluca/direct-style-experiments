package io.github.tassiLuca.analyzer.lib

import gears.async.Future.Collector
import gears.async.{Async, AsyncOperations, Future, Task}
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import io.github.tassiLuca.dse.boundaries.EitherConversions.given
import io.github.tassiLuca.dse.boundaries.either
import io.github.tassiLuca.dse.boundaries.either.?
import io.github.tassiLuca.dse.pimping.TerminableChannelOps.foreach
import io.github.tassiLuca.dse.pimping.asTry

import scala.util.boundary.Label

abstract class AbstractAnalyzer(repositoryService: RepositoryService) extends Analyzer:

  extension (r: Repository)
    protected def performAnalysis(using Async): Task[RepositoryReport] = Task:
      val contributions = Future:
        repositoryService.contributorsOf(r.organization, r.name)
      val release = repositoryService.lastReleaseOf(r.organization, r.name)
      RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.toOption)

private class BasicAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]] = either:
    val reposInfo = repositoryService.repositoriesOf(organizationName).?.map(_.performAnalysis.run)
    val collector = Collector[RepositoryReport](reposInfo.toList*)
    reposInfo.foreach(_ => updateResults(collector.results.read().asTry.?.awaitResult.?))
    reposInfo.awaitAll

private class IncrementalAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]] = either:
    val reposInfo = repositoryService.incrementalRepositoriesOf(organizationName)
    var allReports = Seq[RepositoryReport]()
    var futures = Seq[Future[Unit]]()
    reposInfo.foreach { repository =>
      futures = futures :+ Future:
        val report = repository.?.performAnalysis.run.awaitResult.?
        updateResults(report)
        allReports = allReports :+ report
    }
    futures.awaitAllOrCancel
    allReports
