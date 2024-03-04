package io.github.tassiLuca.analyzer.lib

import cats.data.EitherT
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository}
import monix.eval.Task

/** A service exposing functions to retrieve data from a central hosting repository service. */
trait RepositoryService:

  /** @return a [[EitherT]] encapsulating a [[Task]] which get all the repositories
    *         owned by [[organizationName]].
    */
  def repositoriesOf(organizationName: String): EitherT[Task, String, Seq[Repository]]

  /** @return a [[EitherT]] encapsulating a [[Task]] which get all the contributors
    *         of [[repositoryName]] owned by [[organizationName]].
    */
  def contributorsOf(organizationName: String, repositoryName: String): EitherT[Task, String, Seq[Contribution]]

  /** @return a [[EitherT]] encapsulating a [[Task]] which get the last release
    *         of [[repositoryName]] owned by [[organizationName]].
    */
  def lastReleaseOf(organizationName: String, repositoryName: String): EitherT[Task, String, Release]

object RepositoryService:
  def ofGitHub: RepositoryService = GitHubServiceImpl()

  private class GitHubServiceImpl extends RepositoryService:
    import sttp.client3.httpclient.monix.HttpClientMonixBackend
    import sttp.client3.{UriContext, basicRequest}
    import sttp.model.Uri
    import upickle.default.{Reader, read}

    private val apiUrl = "https://api.github.com"
    private val request = basicRequest.auth.bearer(System.getenv("GH_TOKEN"))

    override def repositoriesOf(organizationName: String): EitherT[Task, String, Seq[Repository]] =
      performRequest[Seq[Repository]](uri"$apiUrl/orgs/$organizationName/repos?per_page=100")

    override def contributorsOf(
        organizationName: String,
        repositoryName: String,
    ): EitherT[Task, String, Seq[Contribution]] =
      performRequest[Seq[Contribution]](uri"$apiUrl/repos/$organizationName/$repositoryName/contributors?per_page=100")

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
