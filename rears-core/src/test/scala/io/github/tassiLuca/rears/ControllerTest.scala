package io.github.tassiLuca.rears

import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.default.given
import gears.async.{Async, AsyncOperations, Channel, Future, ReadableChannel, SendableChannel, Task, TaskSchedule, UnboundedChannel}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Random, Success, Try}

class ControllerTest extends AnyFlatSpec with Matchers:

  "1 to many controller with no transformation" should "broadcast the information to all consumers" in {
    val producer = observable
    val consumers = Set(
      consumer(e => println(s"Consumer A -- $e")),
      consumer(e => println(s"Consumer B -- $e")),
    )
    Async.blocking:
      val p = producer.asRunnable.run
      Controller.oneToMany(producer, consumers, identity).run
      consumers.map(_.asRunnable.run)
      p.await
  }

  def observable: Observable[Int] = new Observable[Int]:
    private val boundarySource = BoundarySource[Int]()
    override def src: Async.Source[Int] = boundarySource
    override def asRunnable: Task[Unit] = Task {
      boundarySource.notifyListeners(Random.nextInt())
    }.schedule(Every(1_000, maxRepetitions = 10))

  def consumer(action: Try[Int] => Unit): Consumer[Try[Int]] = new Consumer[Try[Int]]:
    private val channel = UnboundedChannel[Try[Int]]()
    override def listeningChannel: Channel[Try[Int]] = channel
    override def react(e: Try[Int]): Unit = action(e)
