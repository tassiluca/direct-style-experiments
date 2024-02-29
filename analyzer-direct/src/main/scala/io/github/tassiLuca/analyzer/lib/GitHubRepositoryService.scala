package io.github.tassiLuca.analyzer.lib

import gears.async.{Async, Future, Listener, ReadableChannel, UnboundedChannel}
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository}
import io.github.tassiLuca.pimping.ChannelsPimping.{Terminable, Terminated}

import scala.annotation.tailrec

private class GitHubRepositoryService extends RepositoryService:

  import sttp.model.Uri
  import sttp.client3.{SimpleHttpClient, UriContext, basicRequest, Response}
  import upickle.default.{read, Reader}

  private val baseUrl = "https://api.github.com"
  private val request = basicRequest.auth.bearer(System.getenv("GH_TOKEN"))

  override def repositoriesOf(organizationName: String)(using Async): Either[String, Seq[Repository]] =
    paginatedRequest(uri"$baseUrl/orgs/$organizationName/repos")

  override def incrementalRepositoriesOf(
      organizationName: String,
  )(using Async): ReadableChannel[Terminable[Either[String, Repository]]] =
    incrementalPaginatedRequest(uri"$baseUrl/orgs/$organizationName/repos")

  override def contributorsOf(
      organizationName: String,
      repositoryName: String,
  )(using Async): Either[String, Seq[Contribution]] =
    paginatedRequest(uri"$baseUrl/repos/$organizationName/$repositoryName/contributors")

  override def incrementalContributorsOf(
      organizationName: String,
      repositoryName: String,
  )(using Async): ReadableChannel[Terminable[Either[String, Contribution]]] =
    incrementalPaginatedRequest(uri"$baseUrl/repos/$organizationName/$repositoryName/contributors")
  
  override def lastReleaseOf(organizationName: String, repositoryName: String)(using Async): Either[String, Release] =
    plainRequest[Release](uri"$baseUrl/repos/$organizationName/$repositoryName/releases/latest")

  private def plainRequest[T](endpoint: Uri)(using Reader[T]): Either[String, T] =
    doRequest(endpoint).body.map(read[T](_))

  private def paginatedRequest[T](endpoint: Uri)(using Reader[T]): Either[String, Seq[T]] =
    @tailrec
    def withPagination(partialResponse: Either[String, Seq[T]])(next: Option[Uri]): Either[String, Seq[T]] =
      next match
        case None => partialResponse
        case Some(uri) =>
          val response = doRequest(uri)
          val next = nextPage(response)
          withPagination(partialResponse.flatMap(pr => response.body.map(pr ++ read[Seq[T]](_))))(next)
    withPagination(Right(Seq[T]()))(Some(endpoint))

  private def incrementalPaginatedRequest[T](
      endpoint: Uri,
  )(using Reader[T], Async): ReadableChannel[Terminable[Either[String, T]]] =
    val channel = UnboundedChannel[Terminable[Either[String, T]]]()
    @tailrec
    def withPagination(next: Option[Uri]): Unit = next match
      case None => ()
      case Some(uri) =>
        val response = doRequest(uri)
        response.body.map(read[Seq[T]](_)).fold(
          errorMessage => channel.send(Left(errorMessage)),
          results => results.foreach(r => channel.send(Right(r))),
        )
        withPagination(nextPage(response))
    Future(withPagination(Some(endpoint))).onComplete(Listener((_, _) => channel.send(Terminated)))
    channel.asReadable

  private def doRequest(endpoint: Uri): Response[Either[String, String]] =
    SimpleHttpClient().send(request.get(endpoint))

  private def nextPage(response: Response[Either[String, String]]): Option[Uri] = response.headers("Link")
    .flatMap(_.split(",")).find(_.contains("rel=\"next\""))
    .map(_.takeWhile(_ != ';').trim.stripPrefix("<").stripSuffix(">")).flatMap(Uri.parse(_).toOption)
