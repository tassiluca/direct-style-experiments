package io.github.tassiLuca.rears

import concurrent.duration.DurationInt
import gears.async.Channel.{Closed, Res}
import gears.async.default.given
import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, Future, ReadableChannel, Task, Timer, UnboundedChannel}

type PipelineTransformation[Item] = ReadableChannel[Item] => ReadableChannel[Item]

extension [T](r: ReadableChannel[T])(using Async)
  def filter(p: T => Boolean): ReadableChannel[T] =
    val channel = UnboundedChannel[T]()
    Task {
      val value = r.read().toOption.get
      if p(value) then channel.send(value)
    }.schedule(RepeatUntilFailure()).run
    channel.asReadable

  def buffer(n: Int): ReadableChannel[List[T]] =
    val channel: UnboundedChannel[List[T]] = UnboundedChannel()
    var buffer = List[T]()
    Task {
      val timer = Timer(5.seconds)
      Future { timer.run() }
      val value = Async.raceWithOrigin(r.readSource, timer.src).awaitResult
      if value._2 == timer.src then
        channel.send(buffer)
        buffer = List.empty
      else
        buffer = buffer :+ value._1.asInstanceOf[Either[Closed, T]].toOption.get
        if buffer.size == n then
          channel.send(buffer)
          buffer = List.empty
    }.schedule(RepeatUntilFailure()).run
    channel.asReadable
