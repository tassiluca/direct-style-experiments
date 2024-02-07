package io.github.tassiLuca.rears

import gears.async.TaskSchedule.Every
import gears.async.default.given
import gears.async.{Async, AsyncOperations, Channel, SendableChannel, Task, TaskSchedule, UnboundedChannel}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Random, Try}

class ControllerTest extends AnyFlatSpec with Matchers:

  type Item = Int
  val items = 10

  "1 to many controller" should "broadcast the information to all consumers" in {
//    var consumerAValues = Seq[Int]()
//    var consumerBValues = Seq[Int]()
    val producer = observable
    val consumers = Set(
      consumer(e => println(e) /* consumerAValues = consumerAValues :+ e */ ),
      consumer(e => println(e) /* consumerBValues = consumerBValues :+ e */ ),
    )
    Async.blocking:
      Controller.oneToMany(producer, consumers, identity).run
      consumers.map(_.asRunnable.run)
      producer.asRunnable.run.await // TODO: need to await the consumers!
    // consumerAValues.size shouldBe items
    // consumerBValues.size shouldBe items
  }

  def observable: Observable[Item] = new Observable[Int]:
    private val boundarySource = BoundarySource[Int]()
    override def source: Async.Source[Item] = boundarySource
    override def asRunnable: Task[Unit] = Task {
      boundarySource.notifyListeners(Random.nextInt(10))
    }.schedule(Every(1_000, maxRepetitions = items + 1))

  def consumer(action: Try[Item] => Unit): Consumer[Int] = new Consumer[Int]:
    override val listeningChannel: SendableChannel[Try[Item]] = UnboundedChannel[Try[Int]]()
    override def react(e: Try[Item]): Unit = action(e)
