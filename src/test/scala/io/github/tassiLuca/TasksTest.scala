package io.github.tassiLuca

import gears.async.default.given
import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, Task, TaskSchedule}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Failure

class TasksTest extends AnyFunSpec with Matchers {

  describe("Tasks") {
    describe("when scheduled with RepeatUntilFailure") {
      it("do not leave the Async context if millis = 0 and no suspending calls are performed") {
        var i = 0
        Async.blocking:
          val t = Task {
            i = i + 1
            if i == 5 then Failure(Error()) else i
          }
          t.schedule(TaskSchedule.RepeatUntilFailure()).run
        i shouldBe 5
      }

      describe("when millis > 0 or suspending calls are performed") {
        it("leaves the Async context") {
          var i = 0
          Async.blocking:
            val t = Task {
              i = i + 1
              if i == 5 then Failure(Error()) else i
            }
            t.schedule(TaskSchedule.RepeatUntilFailure(1)).run
          i should be < 5
        }

        it("unless an await is called on the future") {
          var i = 0
          Async.blocking:
            val t = Task {
              i = i + 1
              if i == 5 then Failure(Error()) else i
            }
            t.schedule(TaskSchedule.RepeatUntilFailure(1)).run.await
          i shouldBe 5
        }
      }
    }
  }
}
