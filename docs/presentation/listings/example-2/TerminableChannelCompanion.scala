object TerminableChannel:
  def ofBuffered[T](size: Int) = TerminableChannelImpl(BufferedChannel(size))
  def ofUnbounded[T] = TerminableChannelImpl(UnboundedChannel())

  private class TerminableChannelImpl[T](c: Channel[Terminable[T]]) extends TerminableChannel[T]:
    opaque type Res[R] = Either[Channel.Closed, R]
    private var _terminated: Boolean = false

    override val readSource: Async.Source[Res[Terminable[T]]] =
      c.readSource.transformValuesWith:
        case Right(Terminated) => c.close(); Left(Channel.Closed)
        case v @ _ => v
    
    override def sendSource(x: Terminable[T]): Async.Source[Res[Unit]] =
      synchronized:
        if _terminated then throw ChannelTerminatedException()
        else if x == Terminated then _terminated = true
      c.sendSource(x)
    
    override def close(): Unit = c.close()

    override def terminate()(using Async): Unit =
      try send(Terminated) catch case _: NoSuchElementException => ()