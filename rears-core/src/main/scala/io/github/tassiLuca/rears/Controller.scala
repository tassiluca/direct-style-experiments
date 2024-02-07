package io.github.tassiLuca.rears

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{ChannelMultiplexer, Future, ReadableChannel, SendableChannel, Task, UnboundedChannel}

import scala.util.{Failure, Try}

object Controller:

  def oneToMany[T, R](
      source: Observable[T],
      consumers: Set[Consumer[R]],
      transformation: PipelineTransformation[T, R] = identity,
  ): Task[Unit] = Task:
    val multiplexer = ChannelMultiplexer[R]()
    consumers.foreach(c => multiplexer.addSubscriber(c.listeningChannel))
    Future { multiplexer.run() }
    val channel = UnboundedChannel[T]()
    multiplexer.addPublisher(transformation(channel))
    source.produceOn(channel.asSendable).run.await

  def oneToManyR[T, R](
      source: ReadableChannel[T],
      consumers: Set[Consumer[R]],
      transformation: PipelineTransformation[T, R] = identity,
  ): Task[Unit] = Task:
    val multiplexer = ChannelMultiplexer[R]()
    consumers.foreach(c => multiplexer.addSubscriber(c.listeningChannel))
    Future { multiplexer.run() }
    val channel = UnboundedChannel[T]()
    multiplexer.addPublisher(transformation(channel))
    source.produceOn(channel.asSendable).run.await

  extension [E](o: Observable[E])
    private def produceOn(channel: SendableChannel[E]): Task[Unit] = Task {
      channel.send(o.source.awaitResult)
    }.schedule(RepeatUntilFailure())

  extension [E](o: ReadableChannel[E])
    private def produceOn(channel: SendableChannel[E]): Task[Unit] = Task {
      o.read() match { case Right(value) => channel.send(value); }
    }.schedule(RepeatUntilFailure())
