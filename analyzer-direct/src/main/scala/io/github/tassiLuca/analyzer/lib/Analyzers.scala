package io.github.tassiLuca.analyzer.lib

import gears.async.Future.Collector
import gears.async.{Async, Future}
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import io.github.tassiLuca.boundaries.EitherConversions.given
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.?
import io.github.tassiLuca.pimping.TerminableChannelOps.foreach
import io.github.tassiLuca.pimping.asTry

import scala.util.boundary.Label

abstract class AbstractAnalyzer(repositoryService: RepositoryService) extends Analyzer:

  extension (r: Repository)
    protected def performAnalysis(using Async): Future[RepositoryReport] = Future:
      val contributions = Future {
        repositoryService.contributorsOf(r.organization, r.name)
      }
      val release = repositoryService.lastReleaseOf(r.organization, r.name)
      RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.toOption)

private class BasicAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async): Either[String, Seq[RepositoryReport]] = either:
    val reposInfo = repositoryService.repositoriesOf(organizationName).?.map(_.performAnalysis)
    val collector = Collector[RepositoryReport](reposInfo.toList*)
    for _ <- reposInfo.indices do updateResults(collector.results.read().asTry.?.awaitResult.?)
    reposInfo.map(_.await)

private class IncrementalAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async): Either[String, Seq[RepositoryReport]] = either:
    val reposInfo = repositoryService.incrementalRepositoriesOf(organizationName)
    var allReports = Seq[RepositoryReport]()
    var fs = Seq[Future[Unit]]()
    reposInfo.foreach { repository =>
      val f = Future:
        val report = repository.?.performAnalysis.awaitResult.?
        updateResults(report)
        allReports = allReports :+ report
      fs = fs :+ f
    }
    fs.awaitAllOrCancel
    allReports
