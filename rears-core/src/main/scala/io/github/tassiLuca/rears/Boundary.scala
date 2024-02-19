package io.github.tassiLuca.rears

import gears.async.{Async, Channel, ReadableChannel, SendableChannel, Task, UnboundedChannel}
import gears.async.TaskSchedule.RepeatUntilFailure

import scala.util.Try

trait Publisher[E]:
  protected val channel: Channel[E] = UnboundedChannel[E]()
  def asRunnable: Task[Unit]
  def publishingChannel: ReadableChannel[E] = channel.asReadable

trait Consumer[E]:
  val listeningChannel: SendableChannel[Try[E]] = UnboundedChannel()
  def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach(react)
  }.schedule(RepeatUntilFailure())
  protected def react(e: Try[E])(using Async): Unit

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
