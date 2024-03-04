package io.github.tassiLuca.dse.pimping

import gears.async.TaskSchedule.Every
import gears.async.default.given
import gears.async.{Async, AsyncOperations, Channel, Listener, Task}
import io.github.tassiLuca.dse.pimping.{ChannelTerminatedException, TerminableChannel}
import io.github.tassiLuca.dse.pimping.TerminableChannelOps.{foreach, toSeq}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TerminableChannelTest extends AnyFunSpec with Matchers {

  type Item = Int
  val itemsProduced = 10

  describe("Terminable channels") {
    it("if terminated should close the underlying channel") {
      Async.blocking:
        val channel = TerminableChannel.ofUnbounded[Item]
        channel.terminate()
        channel.read() shouldBe Left(Channel.Closed)
        channel.read() shouldBe Left(Channel.Closed)
    }

    it("attempting to send data after its termination raise a `ChannelTerminatedException`") {
      Async.blocking:
        val channel = TerminableChannel.ofUnbounded[Item]
        channel.terminate()
        intercept[ChannelTerminatedException](channel.send(0))
    }

    it("attempting to terminate it twice raise a `ChannelTerminatedException`") {
      Async.blocking:
        val channel = TerminableChannel.ofUnbounded[Item]
        channel.terminate()
        intercept[ChannelTerminatedException](channel.terminate())
    }

    it("once closed, should be traversable") {
      Async.blocking:
        var collectedItems = Seq[Item]()
        val channel = TerminableChannel.ofUnbounded[Item]
        produceOn(channel).run.onComplete(Listener((_, _) => channel.terminate()))
        channel.foreach(res => collectedItems = collectedItems :+ res)
        collectedItems shouldBe Seq.range(0, itemsProduced)
    }

    it("once closed should be possible to transform it into a Sequence") {
      Async.blocking:
        val channel = TerminableChannel.ofUnbounded[Item]
        produceOn(channel).run.onComplete(Listener((_, _) => channel.terminate()))
        channel.toSeq shouldBe Seq.range(0, itemsProduced)
    }
  }

  def produceOn(channel: TerminableChannel[Item]): Task[Unit] =
    var i = 0
    Task {
      channel.send(i)
      i = i + 1
    }.schedule(Every(100, maxRepetitions = itemsProduced))
}
