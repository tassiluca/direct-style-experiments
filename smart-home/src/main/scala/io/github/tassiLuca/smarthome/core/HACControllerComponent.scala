package io.github.tassiLuca.smarthome.core

trait HACControllerComponent:

  val haccController: HACController

  trait HACController:
    def onHeater(): Unit
    def offHeather(): Unit
    def onAirConditioner(): Unit
    def offAirConditioner(): Unit
