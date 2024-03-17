package io.github.tassiLuca.rears

import gears.async.TaskSchedule.Every
import gears.async.default.given
import gears.async.{Async, Channel, Future, ReadableChannel, Task, TaskSchedule, UnboundedChannel}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration
import concurrent.duration.DurationInt
import scala.language.postfixOps

class PipelineTransformationsTest extends AnyFunSpec with Matchers {

  describe("Filtering a channel") {
    it("return a new channel with only the elements passing the predicate") {
      Async.blocking:
        withResource(producer): c =>
          val filtered = c.filter(_ % 2 == 0)
          for i <- 2 to 10 by 2 do filtered.read() shouldBe Right(i)
    }
  }

  describe("Mapping a channel") {
    it("return a new channel whose values are transformed accordingly to the given function") {
      val f: Int => Int = x => x * x
      Async.blocking:
        withResource(producer): c =>
          val mapped = c.map(f)
          for i <- 1 to 10 do mapped.read() shouldBe Right(f(i))
    }
  }

  describe("Debouncing a channel") {
    it("return a new channel whose first item is emitted immediately") {
      val span = 1.seconds
      Async.blocking:
        withResource(infiniteProducer()): c =>
          val debounced = c.debounce(span)
          val before = System.currentTimeMillis()
          debounced.read()
          val now = System.currentTimeMillis()
          now - before should be < span.toMillis
    }

    it("return a new channel that emit an item if the given timespan has passed without emitting anything") {
      val span = 2.seconds
      val tolerance = 10.milliseconds
      Async.blocking:
        withResource(infiniteProducer()): c =>
          val debounced = c.debounce(span)
          debounced.read()
          val before = System.currentTimeMillis()
          for _ <- 1 to 4 do
            debounced.read()
            val now = System.currentTimeMillis()
            now - before should be > (span.toMillis - tolerance.toMillis)
    }
  }

  describe("Buffering a channel") {
    it("return a new channel that periodically gather items into bundles and emit them") {
      val step = 2
      Async.blocking:
        withResource(producer): c =>
          val buffered = c.buffer(step)
          for i <- 1 to 10 by step do buffered.read() shouldBe Right(List.range(i, i + step))
    }

    it("group fewer items if the nth element is not read within the given timespan") {
      val step = 3
      Async.blocking:
        withResource(producer): c =>
          val buffered = c.buffer(n = step, timespan = 2.seconds)
          for i <- 1 to 9 by step do buffered.read() shouldBe Right(List.range(i, i + step))
          buffered.read() shouldBe Right(List(10))
    }
  }

  describe("Buffering items of a channel within a duration") {
    it("return a new channel emitting items at their first and group next ones emitted until it") {
      Async.blocking:
        val c = UnboundedChannel[Int]()
        infiniteProducer(every = 3000 milliseconds, channel = c)
        infiniteProducer(every = 3500 milliseconds, channel = c)
        val buffered = c.bufferWithin(2 seconds)
        for i <- 0 to 3 do buffered.read() shouldBe Right(List(i, i))
        c.close()
    }
  }

  describe("Grouping a channel on an element selector") {
    it("return a Map with the correct group of channel") {
      Async.blocking:
        withResource(producer): c =>
          val grouped = c.groupBy(_ % 2 == 0)
          for _ <- 0 until 2 do
            val group = grouped.read()
            group.isRight shouldBe true
            group.toOption.get match
              case (false, c) => for i <- 1 to 10 by 2 do c.read() shouldBe Right(i)
              case (true, c) => for i <- 2 to 10 by 2 do c.read() shouldBe Right(i)
    }
  }

  describe("Transforming a channel already closed") {
    it("determine the closing of the new channel") {
      Async.blocking:
        val c = UnboundedChannel[Int]()
        c.close()
        c.filter(_ % 2 == 0).read() shouldBe Left(Channel.Closed)
        c.map(_ * 2).read() shouldBe Left(Channel.Closed)
        c.debounce(1 second).read() shouldBe Left(Channel.Closed)
        c.buffer(2).read() shouldBe Left(Channel.Closed)
        c.bufferWithin(2 seconds).read() shouldBe Left(Channel.Closed)
        c.groupBy(_ % 2 == 0).read() shouldBe Left(Channel.Closed)
    }
  }

  def withResource(channel: Channel[Int])(test: Channel[Int] => Unit): Unit =
    test(channel)
    channel.close()

  def producer(using Async.Spawn): Channel[Int] =
    val channel = UnboundedChannel[Int]()
    Future:
      for i <- 1 to 10 do channel.send(i)
    channel

  def infiniteProducer(
      every: Duration = 500 milliseconds,
      channel: Channel[Int] = UnboundedChannel[Int](),
  )(using Async.Spawn): Channel[Int] =
    var i = 0
    Task:
      channel.send(i)
      i = i + 1
    .schedule(TaskSchedule.Every(every.toMillis)).start()
    channel
}
