package io.github.tassiLuca.analyzer.core

import gears.async.{Async, Future, ReadableChannel}
import gears.async.Future.Collector
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.?

type ReportChannel = (ReadableChannel[Future[RepositoryReport]], Int)

object Analyzer:

  private val gitHubService = GitHubService()

  def analyze(organizationName: String)(using Async): Either[String, ReportChannel] = either:
    val reposInfo = gitHubService
      .organizationsRepositories(organizationName).?
      .map(_.performAnalysis)
      .toList
    (Collector[RepositoryReport](reposInfo: _*).results, reposInfo.size)

  extension (r: Repository)
    private def performAnalysis(using Async): Future[RepositoryReport] = Future:
      val contributions = Future { gitHubService.contributorsOf(r.organization, r.name) }
      val release = Future { gitHubService.lastReleaseOf(r.organization, r.name) }
      RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Set()), release.await.toOption)
