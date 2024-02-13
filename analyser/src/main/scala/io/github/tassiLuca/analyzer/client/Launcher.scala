package io.github.tassiLuca.analyzer.client

import gears.async.TaskSchedule.Every
import gears.async.default.given
import gears.async.*

import scala.util.Random

@main def directAnalyzerLauncher(): Unit =
  Async.blocking:
    AppController()
    Thread.sleep(Long.MaxValue)
