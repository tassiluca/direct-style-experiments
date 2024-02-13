package io.github.tassiLuca.analyzer.core

import gears.async.Async

import scala.util.Properties

trait GitHubService:
  def organizationsRepositories(organizationName: String)(using Async): Set[Repository]
  def contributorsOf(organizationName: String, repositoryName: String)(using Async): Set[Contribution]
  def lastReleaseOf(organizationName: String, repositoryName: String)(using Async): Option[Release]

object GitHubService:
  def apply(): GitHubService = GitHubServiceImpl()

  private class GitHubServiceImpl extends GitHubService:
    import sttp.client3.{SimpleHttpClient, UriContext, basicRequest}
    import upickle.default.read

    private val client = SimpleHttpClient()
    private val bearer = Properties.envOrNone("GH_TOKEN").get

    override def organizationsRepositories(organizationName: String)(using Async): Set[Repository] =
      val endpoint = uri"https://api.github.com/orgs/$organizationName/repos"
      client.send(basicRequest.auth.bearer(bearer).get(endpoint)).body match
        case Left(e) => println(e); Set()
        case Right(response) => read[Seq[Repository]](response).toSet

    override def contributorsOf(organizationName: String, repositoryName: String)(using Async): Set[Contribution] =
      val endpoint = uri"https://api.github.com/repos/$organizationName/$repositoryName/contributors"
      client.send(basicRequest.auth.bearer(bearer).get(endpoint)).body match
        case Left(e) => println(e); Set()
        case Right(response) => read[Seq[Contribution]](response).toSet

    override def lastReleaseOf(organizationName: String, repositoryName: String)(using Async): Option[Release] =
      val endpoint = uri"https://api.github.com/repos/$organizationName/$repositoryName/releases/latest"
      client.send(basicRequest.auth.bearer(bearer).get(endpoint)).body match
        case Left(e) => println(e); None
        case Right(response) => Some(read[Release](response))
