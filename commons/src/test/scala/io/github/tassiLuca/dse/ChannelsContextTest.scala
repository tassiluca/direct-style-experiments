package io.github.tassiLuca.dse

import gears.async.*
import gears.async.Channel.Closed
import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.default.given
import io.github.tassiLuca.dse.pimping.asTry
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class ChannelsContextTest extends AnyFunSpec with Matchers {

  type Item = Int
  val items = 10

  describe("Consumer") {
    it("read no item if the producer is run in another context") {
      var i = 0
      val channel = BufferedChannel[Item](items)
      Async.blocking:
        channel.consume:
          case Left(_) => ()
          case Right(_) => i = i + 1
      Async.blocking:
        produceOn(channel).start().await
      i shouldBe 0
    }

    it("receive a Cancellation exception if a channel is used as a container of futures produced in other process") {
      Async.blocking:
        val channel = UnboundedChannel[Future[Item]]()
        Future:
          for _ <- 0 until items do channel.send(Future { AsyncOperations.sleep(2_000); 0 })
        for _ <- 0 until items do
          val result = channel.read().asTry.flatMap(_.awaitResult)
          result.isFailure shouldBe true
          intercept[CancellationException](result.get)
    }

    it("should work spawning futures and await all") {
      Async.blocking:
        var fs: Seq[Future[Item]] = Seq()
        for _ <- 0 until items do
          fs = fs :+ Future:
            AsyncOperations.sleep(2_000)
            1
        fs.awaitAll.sum shouldBe items
    }
  }

  def produceOn(channel: SendableChannel[Item]): Task[Unit] = Task {
    channel.send(Random.nextInt())
  }.schedule(Every(500, maxRepetitions = items))

  extension (channel: ReadableChannel[Item])
    def consume(action: Either[Closed, Item] => Unit): Task[Unit] = Task {
      action(channel.read())
    }.schedule(RepeatUntilFailure())
}
