package io.github.tassiLuca.boundary.impl

import gears.async.default.given
import gears.async.{Async, Listener}
import io.github.tassiLuca.core.Event
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import concurrent.duration.DurationInt
import scala.language.postfixOps

class TimerTest extends AnyFlatSpec with Matchers:

  "Timer when run" should "generate Tick events observable waiting for them" in {
    val timer = Timer(1 seconds)
    val before = System.currentTimeMillis()
    Async.blocking:
      timer.asRunnable.run
      timer.src.awaitResult shouldBe Event.Tick
    val now = System.currentTimeMillis()
    (now - before) should (be > 1_000L)
  }

  "Timer when run" should "generate Tick events observable attaching a listener" in {
    Async.blocking:
      val timerPeriod = 1 seconds
      val timer = Timer(timerPeriod)
      var event: Event = null
      timer.src.onComplete(Listener((e, _) => event = e))
      timer.asRunnable.run
      Thread.sleep(timerPeriod.toMillis + 100)
      event shouldBe Event.Tick
  }
