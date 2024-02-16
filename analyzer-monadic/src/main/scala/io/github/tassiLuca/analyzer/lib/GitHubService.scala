package io.github.tassiLuca.analyzer.lib

import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository}
import sttp.client3.HttpClientFutureBackend
import sttp.model.Uri

import scala.concurrent.{Await, ExecutionContext, Future}

trait GitHubService:
  def repositoriesOf(organizationName: String)(using ExecutionContext): Future[Either[String, Seq[Repository]]]

  def contributorsOf(
      organizationName: String,
      repositoryName: String,
  )(using ExecutionContext): Future[Either[String, Seq[Contribution]]]

  def lastReleaseOf(
      organizationName: String,
      repositoryName: String,
  )(using ExecutionContext): Future[Either[String, Release]]

object GitHubService:
  def apply(): GitHubService = GitHubServiceImpl()

  private class GitHubServiceImpl extends GitHubService:
    import sttp.client3.{UriContext, basicRequest}
    import upickle.default.{read, Reader}

    private val apiUrl = "https://api.github.com"
    private val request = basicRequest.auth.bearer(System.getenv("GH_TOKEN"))

    override def repositoriesOf(
        organizationName: String,
    )(using ExecutionContext): Future[Either[String, Seq[Repository]]] =
      performRequest[Seq[Repository]](uri"$apiUrl/orgs/$organizationName/repos?per_page=100")

    override def contributorsOf(
        organizationName: String,
        repositoryName: String,
    )(using ExecutionContext): Future[Either[String, Seq[Contribution]]] =
      performRequest[Seq[Contribution]](uri"$apiUrl/repos/$organizationName/$repositoryName/contributors?per_page=100")

    override def lastReleaseOf(
        organizationName: String,
        repositoryName: String,
    )(using ExecutionContext): Future[Either[String, Release]] =
      performRequest[Release](uri"$apiUrl/repos/$organizationName/$repositoryName/releases/latest")

    private def performRequest[T](endpoint: Uri)(using Reader[T], ExecutionContext): Future[Either[String, T]] =
      for
        response <- HttpClientFutureBackend().send(request.get(endpoint))
        result = response.body.map(r => read[T](r))
      yield result

@main def main(): Unit =
  given ExecutionContext = ExecutionContext.global
  val service = GitHubService()
  val result = service.repositoriesOf("unibo-spe")
  Await.ready(result, scala.concurrent.duration.Duration.Inf)
  println(result.value)
  val result2 = service.contributorsOf("unibo-spe", "spe-slides")
  Await.ready(result2, scala.concurrent.duration.Duration.Inf)
  println(result2.value)
  val result3 = service.lastReleaseOf("unibo-spe", "spe-slides")
  Await.ready(result3, scala.concurrent.duration.Duration.Inf)
  println(result3.value)
