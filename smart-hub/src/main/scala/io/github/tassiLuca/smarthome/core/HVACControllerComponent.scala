package io.github.tassiLuca.smarthome.core

import gears.async.Async

trait HVACControllerComponent:

  /** The instance of the HVAC controller, in charge of controlling actuators. */
  val hvacController: HVACController

  /** Heater, Ventilator, Air Conditioner (HVAC) actuators controller. */
  trait HVACController:
    /** Turn on the heater. */
    def onHeater(using Async): Unit

    /** Turn off the heater. */
    def offHeather(using Async): Unit

    /** Turn on the air conditioner. */
    def onAirConditioner(using Async): Unit

    /** Turn on the air conditioner. */
    def offAirConditioner(using Async): Unit
