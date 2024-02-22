package io.github.tassiLuca.utils

import gears.async.default.given
import gears.async.Channel.Closed
import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.{Async, Future, ReadableChannel, SendableChannel, Task, UnboundedChannel}
import io.github.tassiLuca.utils.ChannelsPimping.Terminated
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChannelsPimpingTest extends AnyFlatSpec with Matchers {

  type Item = Int
  val itemsProduced = 10

  "read on a Terminated channel" should "close the channel and stop the task" in {}

//  def produceOn(channel: SendableChannel[Terminated | Item]): Task[Unit] = Task {
//    channel.send(0)
//  }.schedule(Every(500, maxRepetitions = itemsProduced))
//
//  extension (channel: ReadableChannel[Terminated | Item])
//    def consume(action: Either[Closed, Item] => Unit): Task[Unit] = Task {
//      action(channel.read())
//    }.schedule(RepeatUntilFailure())
}
