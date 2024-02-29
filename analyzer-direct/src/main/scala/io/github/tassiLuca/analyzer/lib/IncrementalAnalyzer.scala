package io.github.tassiLuca.analyzer.lib

import gears.async.Future.MutableCollector
import gears.async.{Async, Future}
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.?
import io.github.tassiLuca.boundaries.EitherConversions.given
import io.github.tassiLuca.pimping.ChannelsPimping.toTry
import io.github.tassiLuca.pimping.TerminableChannelOps.foreach

private class IncrementalAnalyzer(repositoryService: RepositoryService) extends Analyzer:

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async): Either[String, Seq[RepositoryReport]] = either:
    val reposInfo = repositoryService.incrementalRepositoriesOf(organizationName)
    val collector = MutableCollector[RepositoryReport]()
    var collectedRepositories = 0
    reposInfo.foreach { repository =>
      collector += repository.?.performAnalysis
      collectedRepositories = collectedRepositories + 1
    }
    var allReports = Seq[RepositoryReport]()
    for _ <- 0 until collectedRepositories do
      val report = collector.results.read().toTry().?.awaitResult.?
      updateResults(report)
      allReports = allReports :+ report
    allReports

  extension (r: Repository)
    private def performAnalysis(using Async): Future[RepositoryReport] = Future:
      val contributions = Future { repositoryService.contributorsOf(r.organization, r.name) }
      val release = repositoryService.lastReleaseOf(r.organization, r.name)
      RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.toOption)
