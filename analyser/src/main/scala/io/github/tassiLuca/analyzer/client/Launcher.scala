package io.github.tassiLuca.analyzer.client

import gears.async.TaskSchedule.Every
import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future, Task}

@main def directAnalyzerLauncher(): Unit =
  AppController()

@main def testCancellation(): Unit =
  Async.blocking:
    var x = 0
    val f1 = Task {
      println(s"Hello gears from first task: $x!")
    }.schedule(Every(1_000)).run
    val f2 = Task { x = x + 1 }.schedule(Every(1_000)).run
    Thread.sleep(10_000)
    Async.current.group.cancel()
    f1.awaitResult
    f2.awaitResult
