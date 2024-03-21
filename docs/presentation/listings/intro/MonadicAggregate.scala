def monadicAggregate(xs: List[Uri]): Either[String, List[String]] =
  xs.foldLeft[Either[String, List[String]]](Right(List.empty)): (acc, uri) =>
    for
      results <- acc
      response <- doRequest(uri)
    yield results :+ response