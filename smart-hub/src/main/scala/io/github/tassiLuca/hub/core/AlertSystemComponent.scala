package io.github.tassiLuca.hub.core

import gears.async.Async

trait AlertSystemComponent:

  /** The alert system instance. */
  val alertSystem: AlertSystem

  /** The alert system port though which is possible to notify alerts. */
  trait AlertSystem:
    def notify(message: String)(using Async): Unit
