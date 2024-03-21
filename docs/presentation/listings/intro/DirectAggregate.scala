def aggregate(xs: List[Uri]): Either[String, List[String]] = either: // boundary
  xs.map(doRequest(_).?)// `?` break if doRequest returns Left

def doRequest(endpoint: Uri): Either[String, String] =
  HttpClientSyncBackend().send(basicRequest.get(endpoint)).body