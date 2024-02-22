package io.github.tassiLuca.smarthome.infrastructure

import gears.async.Async
import io.github.tassiLuca.smarthome.application.ThermostatHubManager
import io.github.tassiLuca.smarthome.core.DashboardComponent

trait MockedThermostatHubManager extends ThermostatHubManager with DashboardComponent:
  override val heater: Heater = new Heater:
    override def on(using Async): Unit = println("[HVAC] Air conditioner turned on")
    override def off(using Async): Unit = println("[HVAC] Heater turned off")

  override val alertSystem: AlertSystem = new AlertSystem:
    override def notify(message: String)(using Async): Unit = println(s"[ALERT-SYSTEM] $message")
