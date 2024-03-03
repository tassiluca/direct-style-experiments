package io.github.tassiLuca

import gears.async.*
import gears.async.Channel.Closed
import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.default.given
import io.github.tassiLuca.pimping.asTry
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Random, Try}

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
          val result = channel.read().asTry.flatMap(_.awaitResult)
          result.isFailure shouldBe true
          intercept[CancellationException](result.get)
    }

    it("but should work putting the send inside a future") {
      Async.blocking:
        val channel = UnboundedChannel[Future[Item]]()
        for _ <- 0 to itemsProduced do
          Future { AsyncOperations.sleep(5_000); 0 }
            .onComplete(Listener((_, f) => channel.send(f.asInstanceOf[Future[Item]])))
        for _ <- 0 to itemsProduced do
          val result = channel.read().asTry.flatMap(_.awaitResult)
          result.isSuccess shouldBe true
          result.get shouldBe 0
    }

    it("instead of future, their results") {
      Async.blocking:
        val channel = UnboundedChannel[Try[Item]]()
        Future:
          var fs: Seq[Future[Item]] = Seq()
          for _ <- 0 to itemsProduced do
            val f = Future { AsyncOperations.sleep(5_000); 100 }
            fs = fs :+ f
            f.onComplete(Listener((r, _) => channel.send(r)))
          fs.awaitAll
        for _ <- 0 to itemsProduced do
          val result = channel.read()
          println(result)
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
