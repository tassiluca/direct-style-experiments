package io.github.tassiLuca.rears

import gears.async.TaskSchedule.Every
import gears.async.default.given
import gears.async.{Async, AsyncOperations, SendableChannel, Task, TaskSchedule, UnboundedChannel}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.Seq
import scala.util.{Success, Try}

class ControllerTest extends AnyFlatSpec with Matchers:

  type Item = Int
  val items = 10

  "1 to many controller" should "broadcast the information to all consumers" in {
    var consumerAValues = Seq[Try[Int]]()
    var consumerBValues = Seq[Try[Int]]()
    val producer = publisher
    val consumers = Set(
      consumer(e => consumerAValues = consumerAValues :+ e),
      consumer(e => consumerBValues = consumerBValues :+ e),
    )
    Async.blocking:
      Controller.oneToMany(producer.publishingChannel, consumers, identity).run
      consumers.foreach(_.asRunnable.run)
      producer.asRunnable.run.await
      // TODO: improve with an extension method that wait for a certain amount of time,
      //        at the expiration of which the channel are closed and stop blocking!
      AsyncOperations.sleep(2_000) // Ensure consumers have completed their reaction to publisher's events
    consumerAValues shouldEqual consumerBValues
    consumerAValues shouldBe Seq.range(0, items).map(Success(_))
    consumerBValues shouldBe Seq.range(0, items).map(Success(_))
  }

  def publisher: Publisher[Item] = new Publisher[Int]:
    private var i = 0
    override def asRunnable: Task[Unit] = Task {
      channel.send(i)
      i = i + 1
    }.schedule(Every(1_000, maxRepetitions = items))

  def consumer(action: Try[Item] => Unit): Consumer[Int] = new Consumer[Int]:
    override val listeningChannel: SendableChannel[Try[Item]] = UnboundedChannel[Try[Int]]()
    override def react(e: Try[Item])(using Async): Unit = action(e)
