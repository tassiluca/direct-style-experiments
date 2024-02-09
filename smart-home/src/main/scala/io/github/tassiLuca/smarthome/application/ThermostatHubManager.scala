package io.github.tassiLuca.smarthome.application

import gears.async.{Async, AsyncOperations, ReadableChannel}
import io.github.tassiLuca.rears.Controller
import io.github.tassiLuca.smarthome.core.{AlertSystemComponent, HVACControllerComponent, SensorHealthCheckerComponent, TemperatureEntry, ThermostatComponent, ThermostatSchedulerComponent}

trait ThermostatHubManager
    extends ThermostatComponent
    with ThermostatSchedulerComponent
    with HVACControllerComponent
    with SensorHealthCheckerComponent[TemperatureEntry]
    with AlertSystemComponent:
  override val scheduler: ThermostatScheduler = ThermostatScheduler(19.0)
  override val thermostat: Thermostat = Thermostat()
  override val sensorHealthChecker: SensorHealthChecker = SensorHealthChecker()

  def run(source: ReadableChannel[TemperatureEntry])(using Async, AsyncOperations): Unit =
    thermostat.asRunnable.run
    sensorHealthChecker.asRunnable.run
    Controller
      .oneToMany(
        source,
        consumers = Set(thermostat, sensorHealthChecker),
        transformation = identity,
      )
      .run
