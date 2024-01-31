package io.github.tassiLuca.rears

import concurrent.duration.{Duration, DurationInt}
import gears.async.Channel.{Closed, Res}
import gears.async.default.given
import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, Channel, Future, ReadableChannel, SendableChannel, Task, Timer, UnboundedChannel}

import scala.language.postfixOps

type PipelineTransformation[Item] = ReadableChannel[Item] => ReadableChannel[Item]

extension [T](r: ReadableChannel[T])(using Async)

  def filter(p: T => Boolean): ReadableChannel[T] =
    val channel = UnboundedChannel[T]()
    Task {
      val value = r.read().toOption.get
      if p(value) then channel.send(value)
    }.schedule(RepeatUntilFailure()).run
    channel.asReadable

  // Strange behavior, apparently the same code but it blocks... see the tests
  def filter2(p: T => Boolean): ReadableChannel[T] =
    as[T] { c =>
      val value = r.read().toOption.get
      if p(value) then c.send(value)
    }

  def debounce(timespan: Duration): ReadableChannel[T] =
    val channel = UnboundedChannel[T]()
    var lastEmission: Option[Long] = None
    Task {
      val value = r.read().toOption.get
      val now = System.currentTimeMillis()
      if lastEmission.isEmpty || now - lastEmission.get >= timespan.toMillis then
        channel.send(value)
        lastEmission = Some(now)
    }.schedule(RepeatUntilFailure()).run
    channel

  def buffer(n: Int, timespan: Duration = 5 seconds): ReadableChannel[List[T]] =
    val channel: UnboundedChannel[List[T]] = UnboundedChannel()
    var buffer = List[T]()
    Task {
      val timer = Timer(timespan)
      Future { timer.run() }
      val value = Async.raceWithOrigin(r.readSource, timer.src).awaitResult
      timer.cancel()
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

private def as[T](transformation: SendableChannel[T] => Unit)(using Async): ReadableChannel[T] =
  val channel = UnboundedChannel[T]()
  Task {
    transformation(channel.asSendable)
  }.schedule(RepeatUntilFailure()).run
  channel.asReadable
