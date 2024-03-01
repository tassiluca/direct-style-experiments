package io.github.tassiLuca

import gears.async.*
import gears.async.Channel.Closed
import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.default.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import io.github.tassiLuca.pimping.ChannelsPimping.toTry

import scala.util.{Failure, Random}

class ChannelsContextTest extends AnyFunSpec with Matchers {

  type Item = Int
  val itemsProduced = 10

  describe("Consumer") {
    it("read no item if the producer is run in another context") {
      var i = 0
      val channel = BufferedChannel[Item](itemsProduced)
      Async.blocking:
        channel.consume {
          case Left(_) => ()
          case Right(_) => i = i + 1
        }
      Async.blocking:
        produceOn(channel).run.await
      i shouldBe 0
    }

    it("receive a Cancellation exception if a channel is used as a container of futures produced in other process") {
      Async.blocking:
        val channel = UnboundedChannel[Future[Item]]()
        Future:
          for _ <- 0 to itemsProduced do channel.send(Future { AsyncOperations.sleep(5_000); 0 })
        for _ <- 0 to itemsProduced do
          val result = channel.read().toTry().flatMap(_.awaitResult)
          result.isFailure shouldBe true
          intercept[CancellationException](result.get)
    }
  }

  /*
    "max repetition on task" should "work" in {
    Async.blocking:
      val channel = UnboundedChannel[Int]()
      Task {
        channel.send(Random.nextInt())
      }.schedule(Every(1000)).run
      Task {
        println(channel.read())
      }.schedule(RepeatUntilFailure(maxRepetitions = 2)).run.await
  }
   */

  def produceOn(channel: SendableChannel[Item]): Task[Unit] = Task {
    channel.send(Random.nextInt())
  }.schedule(Every(500, maxRepetitions = itemsProduced))

  extension (channel: ReadableChannel[Item])
    def consume(action: Either[Closed, Item] => Unit): Task[Unit] = Task {
      action(channel.read())
    }.schedule(RepeatUntilFailure())
}
