package io.github.tassiLuca.pimping

import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import io.github.tassiLuca.pimping.FlowOps.{map, flatMap}

import scala.collection
import scala.collection.immutable
import scala.util.{Try, Success}

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
        flow.collect(value => emitted = emitted :+ value)
        emitted shouldBe Seq.range(0, items).map(Success(_))
    }

    it("calling collect multiple times should emit the same values") {
      var emitted1: Seq[Try[Item]] = Seq()
      var emitted2: Seq[Try[Item]] = Seq()
      Async.blocking:
        val flow = simpleFlow
        flow.collect(value => emitted1 = emitted1 :+ value)
        flow.collect(value => emitted2 = emitted2 :+ value)
        emitted1 shouldBe Seq.range(0, items).map(Success(_))
        emitted2 shouldBe Seq.range(0, items).map(Success(_))
    }

    it("if collected concurrently by multiple Futures should emit the same values as well") {
      Async.blocking:
        var emitted1: Seq[Try[Item]] = Seq()
        var emitted2: Seq[Try[Item]] = Seq()
        val flow = simpleFlow
        val f1 = Future:
          flow.collect(value => emitted1 = emitted1 :+ value)
        val f2 = Future:
          flow.collect(value => emitted2 = emitted2 :+ value)
        (f1 :: f2 :: Nil).awaitAll
        emitted1 shouldBe Seq.range(0, items).map(Success(_))
        emitted2 shouldBe Seq.range(0, items).map(Success(_))
    }

    it("when throwing an exception inside the `body` should emit a failure and stop flowing") {
      Async.blocking:
        var emitted: Seq[Try[Item]] = Seq()
        failingFlow.collect(value => emitted = emitted :+ value)
        emitted.size shouldBe 1
        emitted.head.isFailure shouldBe true
        intercept[IllegalStateException](emitted.head.get)
    }

    it("should work as well with futures") {
      Async.blocking:
        var fs = Seq[Future[Int]]()
        simpleFlow.collect { v =>
          fs = fs :+ Future:
            AsyncOperations.sleep(2_000)
            v.getOrElse(-1)
        }
        fs.awaitAll
        fs.map(_.await) shouldBe Seq.range(0, items)
    }

    it("allows to cancel the task execution inside a collect") {
      Async.blocking:
        val longLastingFlow = Flow:
          (0 until items).foreach { x =>
            AsyncOperations.sleep(1_000); it.emit(x)
          }
        longLastingFlow.collect(v => if v == Success(3) then Async.current.group.cancel() else println(v))
    }

    describe("Flows `map`") {
      it("should work") {
        Async.blocking:
          var emitted: Seq[Try[Item]] = Seq()
          val f = (x: Item) => x * x
          simpleFlow.map(f).collect(value => emitted = emitted :+ value)
          emitted shouldBe Seq.range(0, items).map(f).map(Success(_))
      }

      it("should emit a `Failure` if an exception is thrown in the original flow") {
        Async.blocking:
          var emitted: Seq[Try[Item]] = Seq()
          failingFlow.map(_ * 2).collect(value => emitted = emitted :+ value)
          emitted.size shouldBe 1
          emitted.head.isFailure shouldBe true
          intercept[IllegalStateException](emitted.head.get)
      }

      it("should emit a `Failure` if an exception is thrown in the given function") {
        Async.blocking:
          var emitted: Seq[Try[Item]] = Seq()
          val failingFunction = (_: Item) => throw IllegalStateException()
          simpleFlow.map(failingFunction).collect(value => emitted = emitted :+ value)
          emitted.size shouldBe 1
          emitted.head.isFailure shouldBe true
          intercept[IllegalStateException](emitted.head.get)
      }
    }

    describe("Flows `flatMap`") {
      it("should work") {
        Async.blocking:
          var emitted: Seq[Try[Item]] = Seq()
          val f = (x: Item) => Flow { it.emit(x); it.emit(x + 1) }
          alternatingFlow.flatMap(f).collect(value => emitted = emitted :+ value)
          emitted shouldBe Seq.range(0, items).map(Success(_))
      }

      it("should emit a `Failure` if an exception is thrown in the original flow") {
        Async.blocking:
          var emitted: Seq[Try[Item]] = Seq()
          failingFlow.flatMap(_ => alternatingFlow).collect(value => emitted = emitted :+ value)
          emitted.size shouldBe 1
          emitted.head.isFailure shouldBe true
          intercept[IllegalStateException](emitted.head.get)
      }

      it("should emit a `Failure` if an exception is thrown in the given function") {
        Async.blocking:
          var emitted: Seq[Try[Item]] = Seq()
          failingFlow.flatMap(_ => throw IllegalStateException()).collect(value => emitted = emitted :+ value)
          emitted.size shouldBe 1
          emitted.head.isFailure shouldBe true
          intercept[IllegalStateException](emitted.head.get)
      }
    }
  }

  def simpleFlow(using Async): Flow[Item] = Flow:
    (0 until items).foreach(it.emit(_))

  def alternatingFlow(using Async): Flow[Item] = Flow:
    (0 until items by 2).foreach(it.emit(_))

  def failingFlow(using Async): Flow[Item] = Flow:
    throw IllegalStateException()
    it.emit(10)
