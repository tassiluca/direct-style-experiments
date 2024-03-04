package io.github.tassiLuca.dse.pimping

import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future}
import io.github.tassiLuca.dse.pimping.Flow
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import io.github.tassiLuca.dse.pimping.FlowOps.{flatMap, map}

import scala.collection
import scala.collection.immutable
import scala.util.{Success, Try}

class FlowTest extends AnyFunSpec with Matchers:

  type Item = Int
  val items = 10

  describe("Flows") {
    it("are cold streams") {
      var collected: Seq[Try[Item]] = Seq()
      Async.blocking:
        val flow = simpleFlow
        AsyncOperations.sleep(2_000)
        collected should be(empty)
        flow.collect(value => collected = collected :+ value)
        collected shouldBe Seq.range(0, items).map(Success(_))
    }

    it("calling collect multiple times should emit the same values") {
      var collected1: Seq[Try[Item]] = Seq()
      var collected2: Seq[Try[Item]] = Seq()
      Async.blocking:
        val flow = simpleFlow
        flow.collect(value => collected1 = collected1 :+ value)
        flow.collect(value => collected2 = collected2 :+ value)
      collected1 shouldBe Seq.range(0, items).map(Success(_))
      collected2 shouldBe Seq.range(0, items).map(Success(_))
    }

    it("if collected concurrently by multiple Futures should emit the same values as well") {
      var collected1: Seq[Try[Item]] = Seq()
      var collected2: Seq[Try[Item]] = Seq()
      Async.blocking:
        val flow = simpleFlow
        val f1 = Future:
          flow.collect(value => collected1 = collected1 :+ value)
        val f2 = Future:
          flow.collect(value => collected2 = collected2 :+ value)
        (f1 :: f2 :: Nil).awaitAll
      collected1 shouldBe Seq.range(0, items).map(Success(_))
      collected2 shouldBe Seq.range(0, items).map(Success(_))
    }

    it("when throwing an exception inside the `body` should emit a failure and stop flowing") {
      var collected: Seq[Try[Item]] = Seq()
      Async.blocking:
        failingFlow.collect(value => collected = collected :+ value)
      collected.size shouldBe 1
      collected.head.isFailure shouldBe true
      intercept[IllegalStateException](collected.head.get)
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

    it("allows canceling the task execution inside a collect") {
      var collected: Seq[Try[Item]] = Seq()
      Async.blocking:
        val longLastingFlow = Flow:
          (0 until items).foreach { x =>
            AsyncOperations.sleep(1_000); it.emit(x)
          }
        longLastingFlow.collect { value =>
          if value == Success(3) then Async.current.group.cancel()
          else collected = collected :+ value
        }
      collected shouldBe Seq.range(0, 3).map(Success(_))
    }

    describe("Flows `map`") {
      it("should work") {
        var collected: Seq[Try[Item]] = Seq()
        Async.blocking:
          val f = (x: Item) => x * x
          simpleFlow.map(f).collect(value => collected = collected :+ value)
          collected shouldBe Seq.range(0, items).map(f).map(Success(_))
      }

      it("should emit a `Failure` if an exception is thrown in the original flow") {
        var collected: Seq[Try[Item]] = Seq()
        Async.blocking:
          failingFlow.map(_ * 2).collect(value => collected = collected :+ value)
        collected.size shouldBe 1
        collected.head.isFailure shouldBe true
        intercept[IllegalStateException](collected.head.get)
      }

      it("should emit a `Failure` if an exception is thrown in the given function") {
        var collected: Seq[Try[Item]] = Seq()
        Async.blocking:
          val failingFunction = (_: Item) => throw IllegalStateException()
          simpleFlow.map(failingFunction).collect(value => collected = collected :+ value)
        collected.size shouldBe 1
        collected.head.isFailure shouldBe true
        intercept[IllegalStateException](collected.head.get)
      }
    }

    describe("Flows `flatMap`") {
      it("should work") {
        var collected: Seq[Try[Item]] = Seq()
        Async.blocking:
          val f = (x: Item) => Flow { it.emit(x); it.emit(x + 1) }
          alternatingFlow.flatMap(f).collect(value => collected = collected :+ value)
        collected shouldBe Seq.range(0, items).map(Success(_))
      }

      it("should emit a `Failure` if an exception is thrown in the original flow") {
        var collected: Seq[Try[Item]] = Seq()
        Async.blocking:
          failingFlow.flatMap(_ => alternatingFlow).collect(value => collected = collected :+ value)
        collected.size shouldBe 1
        collected.head.isFailure shouldBe true
        intercept[IllegalStateException](collected.head.get)
      }

      it("should emit a `Failure` if an exception is thrown in the given function") {
        var collected: Seq[Try[Item]] = Seq()
        Async.blocking:
          failingFlow.flatMap(_ => throw IllegalStateException()).collect(value => collected = collected :+ value)
        collected.size shouldBe 1
        collected.head.isFailure shouldBe true
        intercept[IllegalStateException](collected.head.get)
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
