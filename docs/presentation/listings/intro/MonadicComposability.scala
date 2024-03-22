def transform2[E, T](
    xs: Seq[Future[Either[E, T]]],
)(using ec: ExecutionContext): Future[Either[E, Seq[T]]] =
  val initial: Future[Either[E, List[T]]] = Future.successful(Right(List.empty[T]))
  xs.foldRight(initial): (future, acc) =>
    for
      f <- future
      a <- acc
    yield a.flatMap(lst => f.map(_ :: lst))