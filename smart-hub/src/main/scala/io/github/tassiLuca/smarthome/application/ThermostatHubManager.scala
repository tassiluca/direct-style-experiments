package io.github.tassiLuca.smarthome.application

import gears.async.{Async, AsyncOperations, ReadableChannel}
import io.github.tassiLuca.rears.{Controller, bufferWithin}
import io.github.tassiLuca.smarthome.core.{AlertSystemComponent, DashboardComponent, HeaterComponent, SensorHealthCheckerComponent, TemperatureEntry, ThermostatComponent, ThermostatScheduler}

import concurrent.duration.DurationInt
import scala.language.postfixOps

trait ThermostatHubManager
    extends ThermostatComponent
    with HeaterComponent
    with SensorHealthCheckerComponent[TemperatureEntry]
    with AlertSystemComponent
    with DashboardComponent:
  override val thermostat: Thermostat = Thermostat(ThermostatScheduler.byHour(19.0))
  override val sensorHealthChecker: SensorHealthChecker = SensorHealthChecker()

  def run(source: ReadableChannel[TemperatureEntry])(using Async, AsyncOperations): Unit =
    thermostat.asRunnable.run
    sensorHealthChecker.asRunnable.run
    Controller.oneToMany(
      source,
      consumers = Set(thermostat, sensorHealthChecker),
      transformation = r => r.bufferWithin(10.seconds),
    ).run
