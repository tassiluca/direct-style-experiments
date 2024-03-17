package io.github.tassiLuca.rears

import gears.async.Async.Spawn
import gears.async.{Async, Channel, ReadableChannel, SendableChannel, Task, UnboundedChannel}
import gears.async.TaskSchedule.RepeatUntilFailure

import scala.util.Try

/** A producer, i.e. a runnable entity producing items on a channel. */
trait Producer[E]:
  /** The [[Channel]] where specific [[Producer]]s send items to. */
  protected val channel: Channel[E] = UnboundedChannel()

  /** @return the publisher's behavior encoded as a runnable [[Task]]. */
  def asRunnable: Task[Unit]

  /** @return the [[ReadableChannel]] where produced items are placed. */
  def publishingChannel: ReadableChannel[E] = channel.asReadable

/** A consumer, i.e. a runnable entity devoted to consume data from a channel. */
trait Consumer[E, S]:

  /** The [[SendableChannel]] to send items to, where the consumer listen for new items. */
  val listeningChannel: SendableChannel[Try[E]] = UnboundedChannel()

  /** @return a runnable [[Task]]. */
  def asRunnable(using Async.Spawn): Task[Unit] = Task:
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach(react)
  .schedule(RepeatUntilFailure())

  /** The suspendable reaction triggered upon a new read of an item succeeds. */
  protected def react(e: Try[E])(using Async.Spawn): S

/** A mixin to turn consumer stateful. Its state is updated with the result of the [[react]]ion.
  * Initially its state is set to [[initialValue]].
  */
trait State[E, S](initialValue: S):
  consumer: Consumer[E, S] =>

  private var _state: S = initialValue

  /** @return the current state of the consumer. */
  def state: S = synchronized(_state)

  override def asRunnable(using Async.Spawn): Task[Unit] = Task:
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach: e =>
      synchronized:
        _state = react(e)
  .schedule(RepeatUntilFailure())
