package io.github.tassiLuca.analyzer.core

import gears.async.Async

trait GitHubService:
  def repositoriesOf(organizationName: String)(using Async): Either[String, Set[Repository]]
  def contributorsOf(organizationName: String, repositoryName: String)(using Async): Either[String, Set[Contribution]]
  def lastReleaseOf(organizationName: String, repositoryName: String)(using Async): Either[String, Release]

object GitHubService:
  def apply(): GitHubService = GitHubServiceImpl()

  private class GitHubServiceImpl extends GitHubService:
    import sttp.client3.{SimpleHttpClient, UriContext, basicRequest}
    import upickle.default.read

    private val request = basicRequest.auth.bearer(System.getenv("GH_TOKEN"))

    override def repositoriesOf(
        organizationName: String,
    )(using Async): Either[String, Set[Repository]] =
      val endpoint = uri"https://api.github.com/orgs/$organizationName/repos?per_page=100"
      SimpleHttpClient().send(request.get(endpoint)).body.map(r => read[Seq[Repository]](r).toSet)

    override def contributorsOf(
        organizationName: String,
        repositoryName: String,
    )(using Async): Either[String, Set[Contribution]] =
      val endpoint = uri"https://api.github.com/repos/$organizationName/$repositoryName/contributors?per_page=100"
      SimpleHttpClient().send(request.get(endpoint)).body.map(r => read[Seq[Contribution]](r).toSet)

    override def lastReleaseOf(
        organizationName: String,
        repositoryName: String,
    )(using Async): Either[String, Release] =
      val endpoint = uri"https://api.github.com/repos/$organizationName/$repositoryName/releases/latest?per_page=100"
      SimpleHttpClient().send(request.get(endpoint)).body.map(r => read[Release](r))
