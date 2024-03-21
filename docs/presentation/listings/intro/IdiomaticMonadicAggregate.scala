def idiomaticMonadicAggregate(xs: List[Uri]): Either[String, List[String]] =
  import cats.implicits.toTraverseOps
  // "Given a function which returns a G effect, thread this effect through the running of
  // this function on all the values in F, returning an F[B] in a G context."
  //    `def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]]`
  xs.traverse(endpoint => doRequest(endpoint))