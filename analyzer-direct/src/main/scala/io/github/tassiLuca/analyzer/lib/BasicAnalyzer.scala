package io.github.tassiLuca.analyzer.lib

import gears.async.Future.Collector
import gears.async.{Async, Future}
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import io.github.tassiLuca.boundaries.EitherConversions.given
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.?
import io.github.tassiLuca.pimping.ChannelsPimping.toTry

import scala.util.boundary.Label

private class BasicAnalyzer(repositoryService: RepositoryService) extends Analyzer:

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async): Either[String, Seq[RepositoryReport]] = either:
    val reposInfo = repositoryService.repositoriesOf(organizationName).?.map(_.performAnalysis)
    val collector = Collector[RepositoryReport](reposInfo.toList*)
    for _ <- reposInfo.indices do updateResults(collector.results.read().toTry().?.awaitResult.?)
    reposInfo.map(_.await)

  extension (r: Repository)
    private def performAnalysis(using Async): Future[RepositoryReport] = Future:
      val contributions = Future { repositoryService.contributorsOf(r.organization, r.name) }
      val release = repositoryService.lastReleaseOf(r.organization, r.name)
      RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.toOption)
