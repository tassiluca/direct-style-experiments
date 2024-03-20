package io.github.tassiLuca.analyzer.lib

import gears.async.{Async, Future}
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository}
import io.github.tassiLuca.dse.boundaries.{CanFail, either}
import io.github.tassiLuca.dse.pimping.{Flow, TerminableChannel}
import io.github.tassiLuca.dse.boundaries.either.{?, fail}

import scala.annotation.tailrec

private class GitHubRepositoryService extends RepositoryService:

  import sttp.model.Uri
  import sttp.client3.{HttpClientSyncBackend, UriContext, basicRequest, Response}
  import upickle.default.{read, Reader}

  private val baseUrl = "https://api.github.com"
  private val request = basicRequest.auth.bearer(System.getenv("GH_TOKEN"))

  override def repositoriesOf(organizationName: String)(using Async, CanFail): Seq[Repository] =
    paginatedRequest(uri"$baseUrl/orgs/$organizationName/repos")

  override def incrementalRepositoriesOf(
      organizationName: String,
  )(using Async.Spawn): TerminableChannel[Either[String, Repository]] =
    incrementalPaginatedRequest(uri"$baseUrl/orgs/$organizationName/repos")

  override def flowingRepositoriesOf(organizationName: String)(using Async): Flow[Repository] =
    flowingPaginatedRequest(uri"$baseUrl/orgs/$organizationName/repos")

  override def contributorsOf(
      organizationName: String,
      repositoryName: String,
  )(using Async, CanFail): Seq[Contribution] =
    paginatedRequest(uri"$baseUrl/repos/$organizationName/$repositoryName/contributors")

  override def incrementalContributorsOf(
      organizationName: String,
      repositoryName: String,
  )(using Async.Spawn): TerminableChannel[Either[String, Contribution]] =
    incrementalPaginatedRequest(uri"$baseUrl/repos/$organizationName/$repositoryName/contributors")

  override def lastReleaseOf(organizationName: String, repositoryName: String)(using Async, CanFail): Release =
    plainRequest[Release](uri"$baseUrl/repos/$organizationName/$repositoryName/releases/latest").?

  private def plainRequest[T](endpoint: Uri)(using Reader[T]): Either[String, T] =
    doRequest(endpoint).body.map(read[T](_))

  private def paginatedRequest[T](endpoint: Uri)(using Reader[T], CanFail): Seq[T] =
    @tailrec
    def withPagination(partialResponse: Seq[T])(next: Option[Uri]): Seq[T] = next match
      case None => partialResponse
      case Some(uri) =>
        val response = doRequest(uri)
        val body = read[Seq[T]](response.body.getOrElse(fail("Error")))
        val next = nextPage(response)
        withPagination(partialResponse ++ body)(next)
    withPagination(Seq[T]())(Some(endpoint))

  private def incrementalPaginatedRequest[T](
      endpoint: Uri,
  )(using Reader[T], Async.Spawn): TerminableChannel[Either[String, T]] =
    val channel = TerminableChannel.ofUnbounded[Either[String, T]]
    @tailrec
    def withPagination(next: Option[Uri]): Unit = next match
      case None => channel.terminate()
      case Some(uri) =>
        val response = doRequest(uri)
        response.body.map(read[Seq[T]](_)).fold(
          errorMessage => channel.send(Left(errorMessage)),
          results => results.foreach(r => channel.send(Right(r))),
        )
        withPagination(nextPage(response))
    Future(withPagination(Some(endpoint)))
    channel

  private def flowingPaginatedRequest[T](endpoint: Uri)(using Reader[T], Async): Flow[T] = Flow:
    @tailrec
    def withPagination(next: Option[Uri]): Unit = next match
      case None => ()
      case Some(uri) =>
        val response = doRequest(uri)
        response.body.map(read[Seq[T]](_)).fold(
          errorMessage => failWith(errorMessage),
          results => results.foreach(it.emit(_)),
        )
        withPagination(nextPage(response))
    withPagination(Some(endpoint))

  private def doRequest(endpoint: Uri): Response[Either[String, String]] =
    HttpClientSyncBackend().send(request.get(endpoint))

  private def nextPage(response: Response[Either[String, String]]): Option[Uri] = response.headers("Link")
    .flatMap(_.split(",")).find(_.contains("rel=\"next\""))
    .map(_.takeWhile(_ != ';').trim.stripPrefix("<").stripSuffix(">")).flatMap(Uri.parse(_).toOption)

  private def failWith(errorMessage: String): Nothing = throw Exception(errorMessage)
