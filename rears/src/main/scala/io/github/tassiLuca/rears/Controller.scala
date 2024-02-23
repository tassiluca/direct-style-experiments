package io.github.tassiLuca.rears

import gears.async.{Async, ChannelMultiplexer, ReadableChannel, Task}

object Controller:

  def oneToMany[T, R](
      publisherChannel: ReadableChannel[T],
      consumers: Set[Consumer[R, ?]],
      transformation: PipelineTransformation[T, R] = identity,
  ): Task[Unit] = Task:
    val multiplexer = ChannelMultiplexer[R]()
    consumers.foreach(c => multiplexer.addSubscriber(c.listeningChannel))
    multiplexer.addPublisher(transformation(publisherChannel))
    // blocking call: the virtual thread on top of which this task is executed needs to block
    // to continue publishing publisher's events towards the consumer by means of the multiplexer.
    multiplexer.run()
