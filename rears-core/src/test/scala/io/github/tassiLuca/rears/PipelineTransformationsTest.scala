package io.github.tassiLuca.rears

import gears.async.TaskSchedule.Every
import gears.async.default.given
import gears.async.{Async, Future, Listener, ReadableChannel, Task, TaskSchedule, UnboundedChannel}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration
import concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class PipelineTransformationsTest extends AnyFunSpec with Matchers {

  describe("Filtering a channel") {
    it("return a new channel with only the elements passing the predicate") {
      Async.blocking:
        val filtered = producer.filter(_ % 2 == 0)
        for i <- 2 to 10 by 2 do filtered.read() shouldBe Right(i)
    }
  }

  describe("Debouncing a channel") {
    it("return a new channel whose first item is emitted immediately") {
      val span = 1.seconds
      Async.blocking:
        val debounced = infiniteProducer().debounce(span)
        val before = System.currentTimeMillis()
        debounced.read()
        val now = System.currentTimeMillis()
        now - before should be < span.toMillis
    }

    it("return a new channel that emit an item if the given timespan has passed without emitting anything") {
      val span = 2.seconds
      Async.blocking:
        val debounced = infiniteProducer().debounce(span)
        debounced.read()
        for _ <- 1 to 4 do
          val before = System.currentTimeMillis()
          debounced.read()
          val now = System.currentTimeMillis()
          now - before should be > span.toMillis
    }
  }

  describe("Buffering a channel") {
    it("return a new channel that periodically gather items into bundles and emit them") {
      val step = 2
      Async.blocking:
        val buffered = producer.buffer(step)
        for i <- 1 to 10 by step do buffered.read() shouldBe Right(List.range(i, i + step))
    }

    it("group fewer items if the nth element is not read within the given timespan") {
      val step = 3
      Async.blocking:
        val buffered = producer.buffer(n = step, timespan = 2.seconds)
        for i <- 1 to 9 by step do buffered.read() shouldBe Right(List.range(i, i + step))
        buffered.read() shouldBe Right(List(10))
    }
  }

  describe("Grouping a channel on an element selector") {
    it("return a Map with the correct group of channel") {
      Async.blocking:
        val grouped = producer.groupBy(_ % 2 == 0)
        for _ <- 0 until 2 do
          val group = grouped.read()
          group.isRight shouldBe true
          group.toOption.get match
            case (false, c) => for i <- 1 to 10 by 2 do c.read() shouldBe Right(i)
            case (true, c) => for i <- 2 to 10 by 2 do c.read() shouldBe Right(i)
    }
  }

  def producer(using Async): ReadableChannel[Int] =
    val channel = UnboundedChannel[Int]()
    Future { for i <- 1 to 10 do channel.send(i) }
    channel.asReadable

  def infiniteProducer(every: Duration = 500 milliseconds)(using Async): ReadableChannel[Int] =
    val channel = UnboundedChannel[Int]()
    Task(channel.send(Random.nextInt())).schedule(TaskSchedule.Every(every.toMillis)).run
    channel.asReadable
}
