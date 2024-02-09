package io.github.tassiLuca.smarthome.core

trait HVACControllerComponent:

  val hvacController: HVACController

  trait HVACController:
    def onHeater(): Unit
    def offHeather(): Unit
    def onAirConditioner(): Unit
    def offAirConditioner(): Unit
