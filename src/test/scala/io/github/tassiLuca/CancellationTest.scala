package io.github.tassiLuca

import gears.async.default.given
import gears.async.{Async, Future}
import gears.async.AsyncOperations.sleep
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.?
import io.github.tassiLuca.boundaries.EitherConversions.given
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Try}

class CancellationTest extends AnyFlatSpec with Matchers {

  "Structured concurrency with cancellation" should "be supported" in {
    Async.blocking:
      val before = System.currentTimeMillis()
      val f =
        either:
          val f1 = Future { sleep(2_000); "hello" }
          val f2 = Future { throw IllegalStateException() }
          f1.awaitResult.? + f2.awaitResult.?
      println(f)
      val now = System.currentTimeMillis()
      now - before should be > 2_000L
  }

  "Zip" should "**NOT** cancel if the first fails" in {
    val collector = scala.collection.mutable.Set[String]()
    Async.blocking:
      val before = System.currentTimeMillis()
      val f1 = Future { sleep(2_000); collector.add("hello") }
      val f2 = Future { throw IllegalStateException(); collector.add("world") }
      val ftot = f1.zip(f2)
      ftot.awaitResult.isFailure shouldBe true
      val now = System.currentTimeMillis()
      now - before should be < 2_000L
      collector shouldBe empty
      sleep(2_000)
      collector should contain("hello")
  }

  "Await all with cancellation" should "cancel if the first fails" in {
    val collector = scala.collection.mutable.Set[String]()
    Async.blocking:
      val before = System.currentTimeMillis()
      val f1 = Future { sleep(2_000); collector.add("hello") }
      val f2 = Future { throw IllegalStateException(); collector.add("world") }
      val ftot = Try { Seq(f1, f2).awaitAllOrCancel }
      ftot.isFailure shouldBe true
      val now = System.currentTimeMillis()
      now - before should be < 2_000L
      collector shouldBe empty
      sleep(2_000)
      collector shouldBe empty
  }
}
