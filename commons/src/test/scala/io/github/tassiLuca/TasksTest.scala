package io.github.tassiLuca

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.default.given
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

//// "Weird" behaviour
//  "test" should "work" in {
//    Async.blocking:
//      @volatile var end = false
//      val timer = Timer(2 seconds)
//      Future {
//        timer.run()
//      }
//      val f = Future:
//        val tf = Future {
//          timer.src.awaitResult; end = true
//        }
//        val tr = Task {
//          if end then Failure(Error()) else println("hello")
//        }.schedule(RepeatUntilFailure()).run
//        tf.altWithCancel(tr).awaitResult
//      println(f.awaitResult)
//  }
//
//  "test" should "not work" in {
//    Async.blocking:
//      val timer = Timer(2 seconds)
//      Future {
//        timer.run()
//      }
//      val f = Future:
//        val tf = Future {
//          timer.src.awaitResult
//        }
//        val tr = Task {
//          println("hello")
//        }.schedule(RepeatUntilFailure).run // non c'è chiamata bloccante, se ci fosse andrebbe bene
//        tf.altWithCancel(tr).awaitResult
//        tr.cancel()
//      println(f.awaitResult)
//  }

//  object TestCancellation3:
//
//    class Producer3(using Async):
//      val channel = UnboundedChannel[Int]()
//
//      def run(): Future[Unit] = Task {
//        channel.send(Random.nextInt())
//      }.schedule(Every(1_000)).run
//
//      def cancel(): Unit = Async.current.group.cancel()
//
//    @main def testCancellation(): Unit =
//      Async.blocking:
//        val p = Producer3()
//        val f1 = p.run()
//        val f2 = Task {
//          println(s"${p.channel.read()}!")
//        }.schedule(Every(1_000)).run
//        Thread.sleep(10_000)
//        p.cancel()
//        p.run().awaitResult
//
//   def produceOn(channel: SendableChannel[Terminable[Item]]): Task[Unit] =
//    var i = 0
//    Task {
//      println(i)
//      i = i + 1
//      channel.send(i)
//    }.schedule(RepeatUntilFailure(maxRepetitions = itemsProduced))
//
