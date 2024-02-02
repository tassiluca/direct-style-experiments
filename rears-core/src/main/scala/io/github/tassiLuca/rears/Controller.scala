package io.github.tassiLuca.rears

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{ChannelMultiplexer, Future, ReadableChannel, SendableChannel, Task, UnboundedChannel}

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
    source.produceOn(channel.asSendable).run
    val resultChannel = transformation(channel)
    multiplexer.addPublisher(resultChannel)

  extension [E](o: Observable[E])
    private def produceOn(channel: SendableChannel[E]): Task[Unit] = Task {
      channel.send(o.src.awaitResult)
    }.schedule(RepeatUntilFailure())
