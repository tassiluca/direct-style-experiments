object FlowOps:

  extension [T](flow: Flow[T])
    /** @return a new [[Flow]] whose values has been transformed according to [[f]]. */
    def map[R](f: T => R): Flow[R] = new Flow[R]:
      override def collect(collector: Try[R] => Unit)(using Async, AsyncOperations): Unit =
        catchFailure(collector):
          flow.collect(item => collector(Success(f(item.get))))

    /** @return a new [[Flow]] whose values are created by flattening the flows generated
      *         by the given function [[f]] applied to each emitted value of this.
      */
    def flatMap[R](f: T => Flow[R]): Flow[R] = new Flow[R]:
      override def collect(collector: Try[R] => Unit)(using Async, AsyncOperations): Unit =
        catchFailure(collector):
          flow.collect(item => f(item.get).collect(x => collector(Success(x.get))))

    private inline def catchFailure[X](collector: Try[X] => Unit)(inline body: => Unit) =
      try body
      catch case e: Exception => collector(Failure(e))