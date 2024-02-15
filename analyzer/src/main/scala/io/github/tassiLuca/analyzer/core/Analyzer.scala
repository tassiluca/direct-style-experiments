package io.github.tassiLuca.analyzer.core

import gears.async.{Async, Future}
import gears.async.Future.Collector
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.?
import io.github.tassiLuca.boundaries.EitherConversions.given
import io.github.tassiLuca.utils.ChannelClosedConverter.tryable

trait Analyzer:
  def analyze(organizationName: String)(
      updateResults: Async ?=> RepositoryReport => Unit,
  )(using Async): Either[String, Set[RepositoryReport]]

object Analyzer:
  def ofGitHub: Analyzer = GitHubAnalyzer()

  private class GitHubAnalyzer extends Analyzer:
    private val gitHubService = GitHubService()

    override def analyze(organizationName: String)(
        updateResults: Async ?=> RepositoryReport => Unit,
    )(using Async): Either[String, Set[RepositoryReport]] = either:
      val reposInfo = gitHubService
        .repositoriesOf(organizationName).?
        .map(_.performAnalysis)
      val collector = Collector[RepositoryReport](reposInfo.toList*)
      for _ <- 0 until reposInfo.size do updateResults(collector.results.read().tryable.?.awaitResult.?)
      reposInfo.map(_.await)

    extension (r: Repository)
      private def performAnalysis(using Async): Future[RepositoryReport] = Future:
        val contributions = Future { gitHubService.contributorsOf(r.organization, r.name) }
        val release = Future { gitHubService.lastReleaseOf(r.organization, r.name) }
        RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Set()), release.await.toOption)
