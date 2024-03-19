object Flow:
  def apply[T](body: (it: FlowCollector[T]) ?=> Unit): Flow[T] =
    val flow = FlowImpl[T]()
    flow.task = Task:
      val channel = flow.channel
      flow.sync.release()
      given FlowCollector[T] with
        override def emit(value: T)(using Async): Unit = channel.send(Success(value))
      try body catch case e: Exception => channel.send(Failure(e))
    flow

  private class FlowImpl[T] extends Flow[T]:
    private[Flow] var task: Task[Unit] = uninitialized
    private[Flow] var channel: TerminableChannel[Try[T]] = uninitialized
    private[Flow] val sync = Semaphore(0)

    override def collect(collector: Try[T] => Unit)(using Async, AsyncOperations): Unit =
      Async.group:
        val myChannel = TerminableChannel.ofUnbounded[Try[T]]
        synchronized:
            channel = myChannel
            task.start().onComplete(() => myChannel.terminate())
            sync.acquire()
        myChannel.foreach(t => collector(t))