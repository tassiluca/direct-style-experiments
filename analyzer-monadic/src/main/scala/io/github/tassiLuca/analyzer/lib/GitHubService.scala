package io.github.tassiLuca.analyzer.lib

import cats.data.EitherT
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository}
import monix.eval.Task

trait GitHubService:
  def repositoriesOf(organizationName: String): EitherT[Task, String, Seq[Repository]]
  def contributorsOf(organizationName: String, repositoryName: String): EitherT[Task, String, Seq[Contribution]]
  def lastReleaseOf(organizationName: String, repositoryName: String): EitherT[Task, String, Release]

object GitHubService:
  def apply(): GitHubService = GitHubServiceImpl()

  private class GitHubServiceImpl extends GitHubService:
    import sttp.client3.httpclient.monix.HttpClientMonixBackend
    import sttp.client3.{UriContext, basicRequest}
    import sttp.model.Uri
    import upickle.default.{Reader, read}

    private val apiUrl = "https://api.github.com"
    private val request = basicRequest.auth.bearer(System.getenv("GH_TOKEN"))

    override def repositoriesOf(organizationName: String): EitherT[Task, String, Seq[Repository]] =
      performRequest[Seq[Repository]](uri"$apiUrl/orgs/$organizationName/repos")

    override def contributorsOf(
        organizationName: String,
        repositoryName: String,
    ): EitherT[Task, String, Seq[Contribution]] =
      performRequest[Seq[Contribution]](uri"$apiUrl/repos/$organizationName/$repositoryName/contributors")

    override def lastReleaseOf(
        organizationName: String,
        repositoryName: String,
    ): EitherT[Task, String, Release] =
      performRequest[Release](uri"$apiUrl/repos/$organizationName/$repositoryName/releases/latest")

    private def performRequest[T](endpoint: Uri)(using Reader[T]): EitherT[Task, String, T] = EitherT:
      for
        backend <- HttpClientMonixBackend()
        response <- request.get(endpoint).send(backend)
        result = response.body.map(r => read[T](r))
      yield result
