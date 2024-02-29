package io.github.tassiLuca.pimping

import gears.async.default.given
import gears.async.TaskSchedule.Every
import gears.async.{Async, AsyncOperations, Channel, Listener, SendableChannel, Task}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import io.github.tassiLuca.pimping.TerminableChannelOps.foreach

class ChannelsPimpingTest extends AnyFunSpec with Matchers {

  type Item = Int
  val itemsProduced = 10

  describe("Terminable channels") {
    it("if terminated should close the underlying channel") {
      Async.blocking:
        val channel = TerminableChannel.ofUnbounded[Item]
        channel.terminate()
        channel.read() shouldBe Right(Terminated)
        channel.read() shouldBe Left(Channel.Closed)
    }

    it("once closed, should be traversable") {
      Async.blocking:
        var collectedItems = Seq[Item]()
        val channel = TerminableChannel.ofUnbounded[Item]
        produceOn(channel).run.onComplete(Listener { (_, _) => channel.send(Terminated) })
        channel.foreach(res => collectedItems = collectedItems :+ res)
        collectedItems shouldBe Seq.range(0, itemsProduced)
    }

    it("Should again throw") {
      Async.blocking:
        val channel = TerminableChannel.ofUnbounded[Item]
        produceOn(channel).run.onComplete(Listener { (_, _) => channel.send(Terminated) })
        channel.foreach(res =>
          println(s"test3 : $res")
          println(res),
        )
    }
  }

  def produceOn(channel: TerminableChannel[Item]): Task[Unit] =
    var i = 0
    Task {
      channel.send(i)
      i = i + 1
    }.schedule(Every(100, maxRepetitions = itemsProduced))
}
