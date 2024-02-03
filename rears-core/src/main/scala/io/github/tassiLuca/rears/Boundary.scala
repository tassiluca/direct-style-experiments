package io.github.tassiLuca.rears

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.*

trait Observable[E]:
  def src: Async.Source[E]

  def asRunnable: Task[Unit]

trait Consumer[E]:
  def listeningChannel: SendableChannel[E]

  def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[E]].read().foreach(react)
  }.schedule(RepeatUntilFailure())

  def react(e: E): Unit
