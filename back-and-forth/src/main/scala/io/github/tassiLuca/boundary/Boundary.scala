package io.github.tassiLuca.boundary

import gears.async.{Async, Task}
import io.github.tassiLuca.core.Event

trait Boundary:
  def src: Async.Source[Event]
  def asRunnable: Task[Unit]

trait UpdatableBoundary[M] extends Boundary:
  def update(model: M): Unit
