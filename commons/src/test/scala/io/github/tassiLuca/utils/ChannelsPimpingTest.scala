package io.github.tassiLuca.utils

import gears.async.default.given
import gears.async.Channel.Closed
import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.{Async, Future, ReadableChannel, SendableChannel, Task, UnboundedChannel}
import io.github.tassiLuca.utils.ChannelsPimping.{Terminable, Terminated}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ChannelsPimpingTest extends AnyFunSpec with Matchers {

  type Item = Int
  val itemsProduced = 10

  describe("`Terminable` channels") {
    it("allows iterating over them") {
      var collectedResult = Seq[Item]()
      val channel = UnboundedChannel[Terminable[Item]]()
      Async.blocking:
        produceOn(channel.asSendable).run.await
        channel.send(Terminated)
        channel.foreach { item => collectedResult = collectedResult :+ item }
      collectedResult shouldBe Seq.range(0, itemsProduced)
    }
  }

  def produceOn(channel: SendableChannel[Terminable[Item]]): Task[Unit] =
    var i = 0
    Task {
      channel.send(i)
      i = i + 1
    }.schedule(Every(100, maxRepetitions = itemsProduced))
}
