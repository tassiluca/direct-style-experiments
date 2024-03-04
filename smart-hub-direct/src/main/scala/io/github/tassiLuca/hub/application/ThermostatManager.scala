package io.github.tassiLuca.hub.application

import gears.async.{Async, AsyncOperations, ReadableChannel}
import io.github.tassiLuca.hub.core.ports.{AlertSystemComponent, DashboardServiceComponent, HeaterComponent}
import io.github.tassiLuca.hub.core.{
  SensorHealthCheckerComponent,
  TemperatureEntry,
  ThermostatComponent,
  ThermostatHourlyScheduler,
  ThermostatScheduler,
}
import io.github.tassiLuca.rears.{Controller, bufferWithin}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/** The thermostat manager. */
trait ThermostatManager
    extends ThermostatComponent[ThermostatHourlyScheduler]
    with SensorHealthCheckerComponent[TemperatureEntry]
    with HeaterComponent
    with AlertSystemComponent
    with DashboardServiceComponent:
  override val thermostat: Thermostat = Thermostat(ThermostatScheduler.byHour(19))
  override val sensorHealthChecker: SensorHealthChecker = SensorHealthChecker()
  private val samplingWindow = 10 seconds

  /** Runs the manager, spawning a new controller consuming the given [[source]] of events. */
  def run(source: ReadableChannel[TemperatureEntry])(using Async, AsyncOperations): Unit =
    thermostat.asRunnable.run
    sensorHealthChecker.asRunnable.run
    Controller.oneToMany(
      publisherChannel = source,
      consumers = Set(thermostat, sensorHealthChecker),
      transformation = r => r.bufferWithin(samplingWindow),
    ).run
