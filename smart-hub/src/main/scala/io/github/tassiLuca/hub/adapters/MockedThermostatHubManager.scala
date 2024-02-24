package io.github.tassiLuca.hub.adapters

import gears.async.Async
import io.github.tassiLuca.hub.application.ThermostatHubManager
import io.github.tassiLuca.hub.core.ports.DashboardServiceComponent

trait MockedThermostatHubManager extends ThermostatHubManager with DashboardServiceComponent:
  override val heater: Heater = new Heater:
    private var _state = HeaterState.OFF
    override def state: HeaterState = _state
    override def on()(using Async): Unit = println("[Heater] Heater turned on"); _state = HeaterState.ON
    override def off()(using Async): Unit = println("[Heater] Heater turned off"); _state = HeaterState.OFF

  override val alertSystem: AlertSystem = new AlertSystem:
    override def notify(message: String)(using Async): Unit = println(s"[Alert-System] $message")
