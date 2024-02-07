package io.github.tassiLuca.rears

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.*

import scala.util.Try

trait Observable[E]:
  def source: Async.Source[E]

  def asRunnable: Task[Unit]

trait Consumer[E]:
  val listeningChannel: SendableChannel[Try[E]] = UnboundedChannel()

  def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach(react)
  }.schedule(RepeatUntilFailure())

  protected def react(e: Try[E]): Unit

//trait State[E]:
//  consumer: Consumer[E] =>
//
//  private[State] var _state: Option[E] = None
//
//  def state: Option[E] = _state
//
//  override protected def react(e: E): Unit =
//    _state = Some(e)
//    consumer.react(e)
