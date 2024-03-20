override def flowingRepositoriesOf(organizationName: String)(using Async) =
  flowingPaginatedRequest(uri"$baseUrl/orgs/$organizationName/repos")

private def flowingPaginatedRequest[T](endpoint: Uri)(using Reader[T], Async) = 
  Flow: // construct a cold flow
    @tailrec
    def withPagination(next: Option[Uri]): Unit = next match
      case None => ()
      case Some(uri) =>
        val response = doRequest(uri)
        response.body.map(read[Seq[T]](_)).fold(
          errorMessage => failWith(errorMessage),
          results => results.foreach(it.emit(_)), // emit data
        )
        withPagination(nextPage(response))
    withPagination(Some(endpoint))