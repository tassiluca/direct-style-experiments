package io.github.tassiLuca.rears

import gears.async.TaskSchedule.Every
import gears.async.default.given
import gears.async.{Async, Future, ReadableChannel, Task, TaskSchedule, UnboundedChannel}
import org.scalatest.Ignore
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration
import concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class PipelineTransformationsTest extends AnyFlatSpec with Matchers {

  "Debouncing a channel" should "emit the first item immediately" in {
    val span = 1.seconds
    Async.blocking:
      val debounced = infiniteProducer().debounce(span)
      val before = System.currentTimeMillis()
      debounced.read()
      val now = System.currentTimeMillis()
      now - before should be < span.toMillis
  }

  "Debouncing a channel" should "only emit an item if the given timespan has passed without emitting another value" in {
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

  "Filtering a channel" should "return a new channel with only the elements passing the predicate" in {
    Async.blocking:
      val filtered = producer.filter(_ % 2 == 0)
      for i <- 2 to 10 by 2 do filtered.read() shouldBe Right(i)
  }

  ignore /* "filter2" */ should "behave exactly like filter" in {
    Async.blocking:
      val filtered = producer.filter2(_ % 2 == 0)
      for i <- 2 to 10 by 2 do filtered.read() shouldBe Right(i)
  }

  "Buffering a channel" should "periodically gather items emitted by the channel into bundles and emit them" in {
    val step = 2
    Async.blocking:
      val buffered = producer.buffer(step)
      for i <- 1 to 10 by step do buffered.read() shouldBe Right(List.range(i, i + step))
  }

  "Buffering a channel with size not multiple of elements" should "return fewer element" in {
    val step = 3
    Async.blocking:
      val buffered = producer.buffer(step)
      for i <- 1 to 9 by step do buffered.read() shouldBe Right(List.range(i, i + step))
      buffered.read() shouldBe Right(List(10))
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
