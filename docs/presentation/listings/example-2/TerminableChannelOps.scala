object TerminableChannelOps:

  extension [T: ClassTag](c: TerminableChannel[T])

    /** Consume channel items, executing the given function [[f]] for each element. */
    @tailrec
    def foreach[U](f: T => U)(using Async): Unit = c.read() match
      case Left(Channel.Closed) => ()
      case Right(value) =>
        value match
          case Terminated => ()
          case v: T => f(v); foreach(f)

    /** @return a [[Seq]] containing channel items, after having them read. */
    def toSeq(using Async): Seq[T] =
      var results = Seq[T]()
      c.foreach(t => results = results :+ t)
      results