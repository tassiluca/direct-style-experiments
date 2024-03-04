package io.github.tassiLuca.dse

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future, ReadableChannel, SendableChannel, Task, TaskSchedule, Timer, UnboundedChannel}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class TasksTest extends AnyFunSpec with Matchers {

  val items = 5

  describe("Tasks") {
    describe("when scheduled with RepeatUntilFailure") {
      it("do not leave the Async context if millis = 0 and no suspending calls are performed") {
        var i = 0
        Async.blocking:
          Task {
            i = i + 1
            if i == items then Failure(Error()) else i
          }.schedule(TaskSchedule.RepeatUntilFailure()).run
          // millis = 0 is the default --------------Ʌ
        i shouldBe 5
      }

      describe("when millis > 0 or suspending calls are performed") {
        it("leaves the Async context") {
          var i = 0
          Async.blocking:
            Task {
              i = i + 1
              if i == items then Failure(Error()) else i
            }.schedule(TaskSchedule.RepeatUntilFailure(millis = 1)).run
          i should be < items
        }

        it("unless an await is called on the future") {
          var i = 0
          Async.blocking:
            Task {
              i = i + 1
              if i == items then Failure(Error()) else i
            }.schedule(TaskSchedule.RepeatUntilFailure(millis = 1)).run.await
          i shouldBe 5
        }
      }

      describe("with blocking operation and high-order functions") {
        it("if no Async label is present do not leave the Async context") {
          var consumedItems = 0
          Async.blocking:
            val timer = Timer(2.seconds)
            Future(timer.run())
            produce { _ =>
              timer.src.awaitResult
              consumedItems = consumedItems + 1
              if consumedItems == items then Failure(Error()) else Success(())
            }
          consumedItems shouldBe items
        }

        it("with Async label leaves immediately the Async context") {
          var consumedItems = 0
          Async.blocking:
            val timer = Timer(2.seconds)
            Future(timer.run())
            produceWithLabel { _ =>
              timer.src.awaitResult
              consumedItems = consumedItems + 1
              if consumedItems == items then Failure(Error()) else Success(())
            }
          consumedItems should be < items
        }
      }
    }
  }

  def produce[T](action: SendableChannel[T] => Try[Unit])(using Async): ReadableChannel[T] =
    val channel = UnboundedChannel[T]()
    Task {
      action(channel.asSendable)
    }.schedule(RepeatUntilFailure()).run
    channel.asReadable

  def produceWithLabel[T](action: Async ?=> SendableChannel[T] => Try[Unit])(using Async): ReadableChannel[T] =
    val channel = UnboundedChannel[T]()
    Task {
      action(channel.asSendable)
    }.schedule(RepeatUntilFailure()).run
    channel.asReadable
}
