package io.github.tassiLuca.rears

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, ChannelMultiplexer, ReadableChannel, Task}
import io.github.tassiLuca.pimping.toTry

object Controller:

  /** Creates a runnable [[Task]] forwarding the items read from the [[publisherChannel]]
    * to the given [[consumer]], after having it transformed with the given [[transformation]].
    */
  def oneToOne[T, R](
      publisherChannel: ReadableChannel[T],
      consumer: Consumer[R, ?],
      transformation: PipelineTransformation[T, R] = identity,
  ): Task[Unit] =
    val transformedChannel = transformation(publisherChannel)
    Task {
      consumer.listeningChannel.send(transformedChannel.read().toTry())
    }.schedule(RepeatUntilFailure())

  /** Creates a runnable [[Task]] forwarding the items read from the [[publisherChannel]] to
    * all consumers' channels, after having it transformed with the given [[transformation]].
    */
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
