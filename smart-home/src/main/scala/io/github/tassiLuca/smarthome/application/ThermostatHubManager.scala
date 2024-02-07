package io.github.tassiLuca.smarthome.application

import gears.async.{Async, AsyncOperations, ReadableChannel}
import io.github.tassiLuca.rears.Controller
import io.github.tassiLuca.smarthome.core.{
  HACControllerComponent,
  TemperatureEntry,
  ThermostatComponent,
  ThermostatSchedulerComponent,
}

trait ThermostatHubManager extends ThermostatComponent with ThermostatSchedulerComponent with HACControllerComponent:
  override val scheduler: ThermostatScheduler = ThermostatScheduler(19.0)
  override val thermostat: Thermostat = Thermostat()

  def run(source: ReadableChannel[TemperatureEntry])(using Async, AsyncOperations): Unit =
    thermostat.asRunnable.run
    Controller
      .oneToMany(
        source,
        consumers = Set(thermostat),
        transformation = identity,
      )
      .run
