package io.github.tassiLuca

import gears.async.*
import gears.async.Channel.Closed
import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.default.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class ChannelsContextTest extends AnyFunSpec with Matchers {

  type Item = Int
  val itemsProduced = 10

  describe("Channels") {
    it("should receive all produced items") {
      var i = 0
      val channel = BufferedChannel[Int](itemsProduced)
      Async.blocking:
        channel.consume {
          case Left(_) => ()
          case Right(_) => i = i + 1
        }.run
        produceOn(channel).run.await // waiting for producer to finish
        AsyncOperations.sleep(1_000) // making sure consumer finishes
        i shouldBe itemsProduced
    }
  }

  describe("Consumer") {
    it("read no item if the producer is run in another context") {
      var i = 0
      val channel = BufferedChannel[Int](itemsProduced)
      Async.blocking:
        channel.consume {
          case Left(_) => ()
          case Right(_) => i = i + 1
        }
      Async.blocking:
        produceOn(channel).run.await
      i shouldBe 0
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
