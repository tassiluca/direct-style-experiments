package io.github.tassiLuca.rears

import gears.async.default.given
import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, ReadableChannel, Task, UnboundedChannel}

extension [T](source: Async.Source[T])(using Async)
  /** @return a [[ReadableChannel]] which collects the values emitted by the [[source]]. */
  def toChannel: ReadableChannel[T] =
    val channel = UnboundedChannel[T]()
    Task {
      channel.send(source.awaitResult)
    }.schedule(RepeatUntilFailure()).run
    channel.asReadable
