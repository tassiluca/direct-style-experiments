package io.github.tassiLuca.pimping

import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.collection
import scala.collection.immutable
import scala.util.{Try, Success, Failure}

class FlowTest extends AnyFunSpec with Matchers:

  type Item = Int
  val items = 10

  describe("Flows") {
    it("are cold streams") {
      var emitted: Seq[Try[Item]] = Seq()
      Async.blocking:
        val flow = simpleFlow
        AsyncOperations.sleep(2_000)
        emitted should be(empty)
        flow.collect { value =>
          emitted = emitted :+ value
        }
        emitted shouldBe Seq.range(0, items).map(Success(_))
    }

    it("Calling collect multiple times should emit the same values") {
      var emitted1: Seq[Try[Item]] = Seq()
      var emitted2: Seq[Try[Item]] = Seq()
      Async.blocking:
        val flow = simpleFlow
        flow.collect { value => emitted1 = emitted1 :+ value }
        flow.collect { value => emitted2 = emitted2 :+ value }
        emitted1 shouldBe Seq.range(0, items).map(Success(_))
        emitted2 shouldBe Seq.range(0, items).map(Success(_))
    }

    it("If collected concurrently by multiple Futures should emit the same values") {
      Async.blocking:
        var emitted1: Seq[Try[Item]] = Seq()
        var emitted2: Seq[Try[Item]] = Seq()
        val flow = simpleFlow
        val f1 = Future:
          flow.collect { value => emitted1 = emitted1 :+ value }
        val f2 = Future:
          flow.collect { value => emitted2 = emitted2 :+ value }
        (f1 :: f2 :: Nil).awaitAll
        emitted1 shouldBe Seq.range(0, items).map(Success(_))
        emitted2 shouldBe Seq.range(0, items).map(Success(_))
    }

    it("what happens when failing?") {
      Async.blocking:
        var error: Try[Item] = null
        val flow = failingFlow
        flow.collect { value => error = value }
        error.isFailure shouldBe true
        intercept[IllegalStateException](error.get)
    }
  }

  def simpleFlow(using Async): Flow[Item] = Flow:
    (0 until items).foreach(i => summon[FlowCollector[Item]].emit(i))

  def failingFlow(using Async): Flow[Item] = Flow:
    throw IllegalStateException("Something went wrong...")
