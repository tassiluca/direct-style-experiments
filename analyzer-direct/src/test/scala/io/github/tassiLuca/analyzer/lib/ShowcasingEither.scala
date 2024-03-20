package io.github.tassiLuca.analyzer.lib

import io.github.tassiLuca.dse.boundaries.either
import io.github.tassiLuca.dse.boundaries.either.?
import sttp.client3.{HttpClientSyncBackend, Response, basicRequest}
import sttp.model.Uri

def aggregate(xs: List[Uri]): Either[String, List[String]] =
  either: // boundary
    xs.map(doRequest(_).?) // `?` break if doRequest returns a Left

def doRequest(endpoint: Uri): Either[String, String] =
  HttpClientSyncBackend().send(basicRequest.get(endpoint)).body
