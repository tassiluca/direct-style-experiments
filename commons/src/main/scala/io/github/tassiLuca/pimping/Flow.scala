package io.github.tassiLuca.pimping

import gears.async.{Async, AsyncOperations, Task}
import io.github.tassiLuca.pimping.TerminableChannelOps.foreach

import scala.compiletime.uninitialized
import scala.reflect.ClassTag

trait Flow[+T]:
  def collect(collector: FlowCollector[T])(using Async, AsyncOperations): Unit

trait FlowCollector[-T]:
  def emit(value: T)(using Async): Unit

object Flow:
  inline def apply[T: ClassTag](inline body: FlowCollector[T] ?=> Unit)(using Async): Flow[T] =
    val flow = FlowImpl[T]()
    val collector = new FlowCollector[T]:
      override def emit(value: T)(using Async): Unit = flow.channel.send(value)
    flow.task = Task(body(using collector))
    flow

  class FlowImpl[T: ClassTag] extends Flow[T]:
    private[Flow] var task: Task[Unit] = uninitialized

    private[Flow] val channel: TerminableChannel[T] = TerminableChannel.ofUnbounded[T]

    override def collect(collector: FlowCollector[T])(using Async, AsyncOperations): Unit =
      task.run.onComplete(() => channel.terminate())
      channel.foreach(t => collector.emit(t)) // blocking!

object UseFlows:
  import gears.async.default.given

  def simple()(using Async): Flow[Int] = Flow {
    println("Flow started")
    for i <- 0 to 10 do
      AsyncOperations.sleep(1_000)
      summon[FlowCollector[Int]].emit(i)
  }

  @main def main(): Unit = Async.blocking:
    println("Calling simple function...")
    val flow = simple()
    AsyncOperations.sleep(5_000)
    println("OK!")
    val collector = new FlowCollector[Int]:
      override def emit(value: Int)(using Async): Unit = println(value)
    flow.collect(collector)
