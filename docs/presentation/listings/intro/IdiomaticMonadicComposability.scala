def transform[E, T](
    xs: Seq[Future[Either[E, T]]],
)(using ExecutionContext): Future[Either[E, Seq[T]]] =
  import cats.implicits._
  Future.sequence(xs) // Future[Seq[Either[E, T]]
    .map(_.sequence) // equivalent to: _.traverse(identity)
