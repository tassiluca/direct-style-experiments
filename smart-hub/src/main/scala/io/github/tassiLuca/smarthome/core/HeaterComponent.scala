package io.github.tassiLuca.smarthome.core

import gears.async.Async

trait HeaterComponent:

  /** The instance in charge of controlling heater actuator. */
  val heater: Heater

  /** Heater actuator controller. */
  trait Heater:
    /** Turn on the heater. */
    def on(using Async): Unit

    /** Turn off the heater. */
    def off(using Async): Unit
