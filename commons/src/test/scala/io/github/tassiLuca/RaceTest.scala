package io.github.tassiLuca

import gears.async.default.given
import gears.async.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class RaceTest extends AnyFlatSpec with Matchers {

  "Racing a timer and a long-running task" should "return the timer first" in {
    Async.blocking:
      val timer = Timer(2 seconds)
      Future { timer.run() } // run is a blocking operation
      val firstSource = Async.raceWithOrigin(
        timer.src,
        Future { AsyncOperations.sleep(10.seconds.toMillis); "hello" },
      )
      firstSource.awaitResult._2 shouldBe timer.src
  }
}
