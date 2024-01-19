package io.github.tassiLuca.boundary.impl

import gears.async.{Async, Task}
import gears.async.default.given
import io.github.tassiLuca.boundary.Boundary
import io.github.tassiLuca.core.Event

import scala.concurrent.duration.Duration

class Timer(period: Duration) extends Boundary:
  private val timer = gears.async.Timer(period)

  override def src: Async.Source[Event] =
    timer.src.transformValuesWith(_ => Event.Tick)

  override def asRunnable: Task[Unit] = Task(timer.run())
