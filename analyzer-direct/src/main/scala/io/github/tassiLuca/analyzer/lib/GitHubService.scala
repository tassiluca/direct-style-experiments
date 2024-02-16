package io.github.tassiLuca.analyzer.lib

import gears.async.Async
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository}

trait GitHubService:
  def repositoriesOf(organizationName: String)(using Async): Either[String, Seq[Repository]]
  def contributorsOf(organizationName: String, repositoryName: String)(using Async): Either[String, Seq[Contribution]]
  def lastReleaseOf(organizationName: String, repositoryName: String)(using Async): Either[String, Release]

object GitHubService:
  def apply(): GitHubService = GitHubServiceImpl()

  private class GitHubServiceImpl extends GitHubService:
    import sttp.client3.{SimpleHttpClient, UriContext, basicRequest}
    import upickle.default.read

    private val request = basicRequest.auth.bearer(System.getenv("GH_TOKEN"))

    override def repositoriesOf(
        organizationName: String,
    )(using Async): Either[String, Seq[Repository]] =
      val endpoint = uri"https://api.github.com/orgs/$organizationName/repos?per_page=100"
      SimpleHttpClient().send(request.get(endpoint)).body.map(r => read[Seq[Repository]](r))

    override def contributorsOf(
        organizationName: String,
        repositoryName: String,
    )(using Async): Either[String, Seq[Contribution]] =
      val endpoint = uri"https://api.github.com/repos/$organizationName/$repositoryName/contributors?per_page=100"
      SimpleHttpClient().send(request.get(endpoint)).body.map(r => read[Seq[Contribution]](r))

    override def lastReleaseOf(
        organizationName: String,
        repositoryName: String,
    )(using Async): Either[String, Release] =
      val endpoint = uri"https://api.github.com/repos/$organizationName/$repositoryName/releases/latest?per_page=100"
      SimpleHttpClient().send(request.get(endpoint)).body.map(r => read[Release](r))
