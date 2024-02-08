package io.github.tassiLuca

import gears.async.AsyncOperations.sleep
import gears.async.default.given
import gears.async.{Async, AsyncOperations, Task}
import gears.async.TaskSchedule.RepeatUntilFailure

def t2 = Task {
  println(s"-> ${t.run}")
  println("Here")
}

def t = Task {
  println("Simple task :)")
  sleep(1_000)
  println("Ending iteration")
}.schedule(RepeatUntilFailure())

@main def use(): Unit =
  Async.blocking:
    println("Initializing first")
    val first = t2
    AsyncOperations.sleep(10_000)
    println("Starting first")
    first.run.await
