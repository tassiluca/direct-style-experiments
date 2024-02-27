package io.github.tassiLuca.hub.core.ports

import gears.async.Async

/** The component encapsulating the lamps controller port. */
trait LampsComponent:

  /** The [[LampsController]] instance. */
  val lamps: LampsController

  /** The lamps controller port through which is possible to turn on and off lamps. */
  trait LampsController:
    /** Turning on the lamps. */
    def on()(using Async): Unit

    /** Turning off the lamps. */
    def off()(using Async): Unit
