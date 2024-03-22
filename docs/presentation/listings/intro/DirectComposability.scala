def transform[E, T](
    xs: Seq[Future[Either[E, T]]]
)(using Async.Spawn): Future[Either[E, Seq[T]]] =
  Future:
    either:
      xs.map(_.await.?)