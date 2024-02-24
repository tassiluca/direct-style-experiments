package io.github.tassiLuca.hub.application

import gears.async.{Async, AsyncOperations, ReadableChannel}
import io.github.tassiLuca.hub.core.ports.{AlertSystemComponent, DashboardServiceComponent, HeaterComponent}
import io.github.tassiLuca.hub.core.{
  SensorHealthCheckerComponent,
  TemperatureEntry,
  ThermostatComponent,
  ThermostatScheduler,
}
import io.github.tassiLuca.rears.{Controller, bufferWithin}

import concurrent.duration.DurationInt
import scala.language.postfixOps

trait ThermostatHubManager
    extends ThermostatComponent
    with SensorHealthCheckerComponent[TemperatureEntry]
    with HeaterComponent
    with AlertSystemComponent
    with DashboardServiceComponent:
  override val thermostat: Thermostat = Thermostat(ThermostatScheduler.byHour(19))
  override val sensorHealthChecker: SensorHealthChecker = SensorHealthChecker()

  def run(source: ReadableChannel[TemperatureEntry])(using Async, AsyncOperations): Unit =
    thermostat.asRunnable.run
    sensorHealthChecker.asRunnable.run
    Controller.oneToMany(
      publisherChannel = source,
      consumers = Set(thermostat, sensorHealthChecker),
      transformation = r => r.bufferWithin(10.seconds),
    ).run
