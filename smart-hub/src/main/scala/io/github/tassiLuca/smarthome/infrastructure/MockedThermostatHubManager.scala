package io.github.tassiLuca.smarthome.infrastructure

import gears.async.Async
import io.github.tassiLuca.smarthome.application.ThermostatHubManager
import io.github.tassiLuca.smarthome.core.DashboardComponent

trait MockedThermostatHubManager extends ThermostatHubManager with DashboardComponent:
  override val heater: Heater = new Heater:
    private var _state = HeaterState.OFF
    override def state: HeaterState = _state
    override def on()(using Async): Unit = println("[Heater] Heater turned on"); _state = HeaterState.ON
    override def off()(using Async): Unit = println("[Heater] Heater turned off"); _state = HeaterState.OFF

  override val alertSystem: AlertSystem = new AlertSystem:
    override def notify(message: String)(using Async): Unit = println(s"[ALERT-SYSTEM] $message")
