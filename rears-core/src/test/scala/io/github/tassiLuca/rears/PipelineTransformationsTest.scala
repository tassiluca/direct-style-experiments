package io.github.tassiLuca.rears

import gears.async.default.given
import gears.async.{Async, Future, ReadableChannel, UnboundedChannel}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PipelineTransformationsTest extends AnyFlatSpec with Matchers {

  "Filtering a channel" should "return a new channel with only the elements passing the predicate" in {
    Async.blocking:
      val filtered = producer.filter(_ % 2 == 0)
      for i <- 2 to 10 by 2 do filtered.read() shouldBe Right(i)
  }

  "Buffering a channel" should "periodically gather items emitted by the channel into bundles and emit them" in {
    val step = 2
    Async.blocking:
      val buffered = producer.buffer(step)
      for i <- 1 to 10 by step do buffered.read() shouldBe Right(List.range(i, i + step))
  }

  "Buffering a channel with size not multiple of elements" should "return fewer element in the last bunch" in {
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
}
