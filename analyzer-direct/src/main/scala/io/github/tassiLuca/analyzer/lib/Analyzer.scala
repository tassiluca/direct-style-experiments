package io.github.tassiLuca.analyzer.lib

import gears.async.Future.Collector
import gears.async.{Async, Future}
import io.github.tassiLuca.analyzer.commons.lib
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import io.github.tassiLuca.boundaries.EitherConversions.given
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.?
import io.github.tassiLuca.utils.ChannelsPimping.tryable

trait Analyzer:
  def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async): Either[String, Seq[RepositoryReport]]

object Analyzer:
  def of(service: GitHubService): Analyzer = GitHubAnalyzer(service)

  private class GitHubAnalyzer(gitHubService: GitHubService) extends Analyzer:

    override def analyze(organizationName: String)(
        updateResults: RepositoryReport => Unit,
    )(using Async): Either[String, Seq[RepositoryReport]] = either:
      val reposInfo = gitHubService
        .repositoriesOf(organizationName).?
        .map(_.performAnalysis)
      val collector = Collector[RepositoryReport](reposInfo.toList*)
      for _ <- reposInfo.indices do updateResults(collector.results.read().tryable.?.awaitResult.?)
      reposInfo.map(_.await)

    extension (r: Repository)
      private def performAnalysis(using Async): Future[RepositoryReport] = Future:
        val contributions = Future { gitHubService.contributorsOf(r.organization, r.name) }
        val release = Future { gitHubService.lastReleaseOf(r.organization, r.name) }
        RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.await.toOption)
