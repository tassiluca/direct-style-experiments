package io.github.tassiLuca.smarthome.core

import gears.async.Async

trait HVACControllerComponent:

  val hvacController: HVACController

  trait HVACController:
    def onHeater(using Async): Unit
    def offHeather(using Async): Unit
    def onAirConditioner(using Async): Unit
    def offAirConditioner(using Async): Unit
