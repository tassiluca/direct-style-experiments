package io.github.tassiLuca.rears

import gears.async.{Async, Channel, ReadableChannel, SendableChannel, Task, UnboundedChannel}
import gears.async.TaskSchedule.RepeatUntilFailure

import scala.util.Try

/** A publisher, i.e. a runnable entity producing items on a channel. */
trait Publisher[E]:
  /** The [[Channel]] where specific [[Publisher]]s send items to. */
  protected val channel: Channel[E] = UnboundedChannel()

  /** @return the publisher's behavior encoded as a runnable [[Task]]. */
  def asRunnable: Task[Unit]

  /** @return the [[ReadableChannel]] where produced items are placed. */
  def publishingChannel: ReadableChannel[E] = channel.asReadable

/** A consumer, i.e. a runnable entity devoted to consume data from a channel. */
trait Consumer[E]:

  /** The [[SendableChannel]] to send items to, where consumers listen for new items. */
  val listeningChannel: SendableChannel[Try[E]] = UnboundedChannel()

  /** @return a runnable [[Task]]. */
  def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach(react)
  }.schedule(RepeatUntilFailure())

  /** The suspendable reaction triggered upon a new read of an item succeeds. */
  protected def react(e: Try[E])(using Async): Unit

/** A mixin to make consumers stateful. */
trait State[E]:
  consumer: Consumer[E] =>

  private var _state: Option[E] = None

  def state: Option[E] = synchronized(_state)

  override def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach { e =>
      react(e)
      synchronized { _state = e.toOption }
    }
  }.schedule(RepeatUntilFailure())
