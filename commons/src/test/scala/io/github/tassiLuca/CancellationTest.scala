package io.github.tassiLuca

import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.CancellationException
import scala.util.Failure

class CancellationTest extends AnyFunSpec with Matchers {

  describe("Cancellation of a `Future`") {
    it("can be achieve calling `cancel` method on it") {
      Async.blocking:
        var terminated = false
        val f = Future:
          AsyncOperations.sleep(5_000)
          terminated = true
        AsyncOperations.sleep(1_000)
        f.cancel()
        val result = f.awaitResult
        result.isFailure shouldBe true
        intercept[CancellationException](result.get)
        terminated shouldBe false
    }
  }

  describe("Cancellation of an Async context") {
    it("can be achieve calling the `cancel` method on the current group") {
      var terminated = false
      Async.blocking:
        val before = System.currentTimeMillis()
        val f = Future:
          AsyncOperations.sleep(5_000)
          terminated = true
        AsyncOperations.sleep(1_000)
        Async.current.group.cancel()
        Async.current.group.isCancelled shouldBe true
        f.awaitResult.isFailure shouldBe true
        val now = System.currentTimeMillis()
        (now - before) should (be > 1_000L and be < 5_000L)
      terminated shouldBe false
    }

    it("once the context is cancelled no future is completed") {
      Async.blocking:
        Async.current.group.cancel()
        val before = System.currentTimeMillis()
        val f = Future:
          AsyncOperations.sleep(5_000)
        f.awaitResult.isFailure shouldBe true
        val now = System.currentTimeMillis()
        (now - before) should be < 5_000L
    }
  }
}
