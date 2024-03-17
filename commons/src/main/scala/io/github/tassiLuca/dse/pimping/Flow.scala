package io.github.tassiLuca.dse.pimping

import gears.async.{Async, AsyncOperations, Task}
import io.github.tassiLuca.dse.pimping.ListenerConversions.given
import io.github.tassiLuca.dse.pimping.TerminableChannelOps.foreach

import java.util.concurrent.Semaphore
import scala.compiletime.uninitialized
import scala.reflect.ClassTag
import scala.util.boundary.break
import scala.util.{Failure, Success, Try, boundary}

/** An asynchronous cold data stream that emits values, inspired to Kotlin Flows. */
trait Flow[+T]:

  /** Start the flowing of data which can be collected reacting through the given [[collector]] function. */
  def collect(collector: Try[T] => Unit)(using Async, AsyncOperations): Unit

/** An interface modeling an entity capable of [[emit]]ting [[Flow]]able values. */
trait FlowCollector[-T]:

  /** Emits a value to the flow. */
  def emit(value: T)(using Async): Unit

object Flow:

  /** Creates a new asynchronous cold [[Flow]] from the given [[body]].
    * Since it is cold, it starts emitting values only when the [[Flow.collect]] method is called.
    * To emit a value use the [[FlowCollector]] given instance.
    */
  def apply[T](body: (it: FlowCollector[T]) ?=> Unit): Flow[T] =
    val flow = FlowImpl[T]()
    flow.task = Task:
      val channel = flow.channel
      flow.sync.release()
      given FlowCollector[T] with
        override def emit(value: T)(using Async): Unit = channel.send(Success(value))
      try body
      catch case e: Exception => channel.send(Failure(e))
    flow

  private class FlowImpl[T] extends Flow[T]:
    private[Flow] var task: Task[Unit] = uninitialized
    private[Flow] var channel: TerminableChannel[Try[T]] = uninitialized
    private[Flow] val sync = Semaphore(0)

    override def collect(collector: Try[T] => Unit)(using Async, AsyncOperations): Unit = Async.group:
      val myChannel = TerminableChannel.ofUnbounded[Try[T]]
      synchronized:
        channel = myChannel
        task.start().onComplete(() => myChannel.terminate())
        // Ensure to leave the synchronized block after the task has been initialized
        // with the correct channel instance.
        sync.acquire()
      myChannel.foreach(t => collector(t))

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
          
    def toSeq(using Async, AsyncOperations): Try[Seq[T]] = boundary:
      var result = Seq.empty[T]
      flow.collect:
        case Success(value) => result = result :+ value
        case e => break(e.asInstanceOf[Try[Seq[T]]])
      Success(result)

    private inline def catchFailure[X](collector: Try[X] => Unit)(inline body: => Unit): Unit =
      try body
      catch case e: Exception => collector(Failure(e))
