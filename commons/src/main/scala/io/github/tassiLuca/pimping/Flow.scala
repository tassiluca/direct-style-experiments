package io.github.tassiLuca.pimping

import gears.async.{Async, AsyncOperations, Task}
import io.github.tassiLuca.pimping.TerminableChannelOps.foreach
import io.github.tassiLuca.pimping.ListenerConversions.given
import java.util.concurrent.Semaphore
import scala.compiletime.uninitialized
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait Flow[+T]:
  def collect(collector: Try[T] => Unit)(using Async, AsyncOperations): Unit

trait FlowCollector[-T]:
  def emit(value: T)(using Async): Unit

object Flow:
  def apply[T: ClassTag](body: FlowCollector[T] ?=> Unit)(using Async): Flow[T] =
    val flow = FlowImpl[T]()
    flow.task = Task:
      val channel = flow.channel
      flow.sync.release()
      val collector: FlowCollector[T] = new FlowCollector[T]:
        override def emit(value: T)(using Async): Unit = channel.send(Success(value))
      try body(using collector)
      catch case e: Exception => channel.send(Failure(e))
    flow

  private class FlowImpl[T: ClassTag] extends Flow[T]:
    private[Flow] var task: Task[Unit] = uninitialized
    private[Flow] var channel: TerminableChannel[Try[T]] = uninitialized
    private[Flow] val sync = Semaphore(0)

    override def collect(collector: Try[T] => Unit)(using Async, AsyncOperations): Unit =
      val myChannel = TerminableChannel.ofUnbounded[Try[T]]
      synchronized:
        channel = myChannel
        task.run.onComplete(() => myChannel.terminate())
        sync.acquire()
      myChannel.foreach(t => collector(t)) // blocking!
