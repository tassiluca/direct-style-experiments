package io.github.tassiLuca.rears

import gears.async.default.given
import gears.async.{Async, Future, Timer}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class AsyncPimpingTest extends AnyFlatSpec with Matchers {

  "toChannel" should "return a ReadableChannel with the values of the source" in {
    val timerSource = Timer(250.milliseconds)
    val before = System.currentTimeMillis()
    Async.blocking:
      val channel = timerSource.src.toChannel
      Future { timerSource.run() }
      for _ <- 1 to 4 do channel.read().isRight shouldBe true
    val now = System.currentTimeMillis()
    now - before should (be > 1_000L and be < 1_250L)
  }
}
