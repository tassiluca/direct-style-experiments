package io.github.tassiLuca.smarthome.infrastructure

import gears.async.Async
import io.github.tassiLuca.smarthome.application.ThermostatHubManager

class MockedThermostatHubManager extends ThermostatHubManager:
  override val hvacController: HVACController = new HVACController:
    override def onAirConditioner(using Async): Unit = println("[HVAC] Air conditioner turned on")
    override def offAirConditioner(using Async): Unit = println("[HVAC] Air conditioner turned off")
    override def offHeather(using Async): Unit = println("[HVAC] Heater turned off")
    override def onHeater(using Async): Unit = println("[HVAC] Heater turned on")

  override val alertSystem: AlertSystem = new AlertSystem:
    override def notify(message: String)(using Async): Unit = println(s"[ALERT-SYSTEM] $message")
