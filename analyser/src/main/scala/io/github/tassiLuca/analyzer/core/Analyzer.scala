package io.github.tassiLuca.analyzer.core

import gears.async.{Async, AsyncOperations, Future, ReadableChannel, Task}
import gears.async.Future.Collector

object Analyzer:

  private val gitHubService = GitHubService()

  def analyze(
      organizationName: String,
  )(using Async, AsyncOperations): (Int, ReadableChannel[Future[RepositoryReport]]) =
    val reposInfo = gitHubService
      .organizationsRepositories(organizationName)
      .map(_.performAnalysis(organizationName).run)
      .toList
    (reposInfo.size, Collector[RepositoryReport](reposInfo: _*).results)

  extension (repository: Repository)
    private def performAnalysis(organizationName: String): Task[RepositoryReport] = Task:
      val contributions = Future { gitHubService.contributorsOf(organizationName, repository.name) }
      // val release = Future { gitHubService.lastReleaseOf(organizationName, repository.name) }
      RepositoryReport(repository.name, repository.issues, repository.stars, contributions.await, None)
