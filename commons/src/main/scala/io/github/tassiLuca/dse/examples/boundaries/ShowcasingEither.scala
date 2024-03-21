package io.github.tassiLuca.dse.examples.boundaries

import io.github.tassiLuca.dse.boundaries.either
import io.github.tassiLuca.dse.boundaries.either.?
import sttp.client3.{HttpClientSyncBackend, UriContext, basicRequest}
import sttp.model.Uri

trait ShowcasingEither:

  def aggregate(xs: List[Uri]): Either[String, List[String]] = either: // boundary
    xs.map(doRequest(_).?) // `?` break if doRequest returns a Left

  def monadicAggregate(xs: List[Uri]): Either[String, List[String]] =
    xs.foldLeft[Either[String, List[String]]](Right(List.empty)): (acc, uri) =>
      for
        results <- acc
        response <- doRequest(uri)
      yield results :+ response

  def idiomaticMonadicAggregate(xs: List[Uri]): Either[String, List[String]] =
    import cats.implicits.toTraverseOps
    // "Given a function which returns a G effect, thread this effect through the running of
    // this function on all the values in F, returning an F[B] in a G context."
    //
    //    def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]]
    xs.traverse(doRequest)

  def doRequest(endpoint: Uri): Either[String, String]
  // a possible implementation:
  //  HttpClientSyncBackend().send(basicRequest.get(endpoint)).body

trait TestShowcasingEither extends ShowcasingEither with App:
  val uris = List(uri"https://www.google.com", uri"https://www.bing.com")
  println(aggregate(uris))
  println(monadicAggregate(uris))
  println(idiomaticMonadicAggregate(uris))

object TestRightShowcasingEither extends TestShowcasingEither:
  override def doRequest(endpoint: Uri): Either[String, String] = Right("Ok")

object TestLeftShowcasingEither extends TestShowcasingEither:
  override def doRequest(endpoint: Uri): Either[String, String] = Left("Ouch!")
