package io.github.tassiLuca.smarthome.core

import scala.util.Try
import io.github.tassiLuca.rears.{Consumer, State}

trait ThermostatComponent:
  context: ThermostatSchedulerComponent with HVACControllerComponent =>

  val thermostat: Thermostat

  /** The entity in charge of controlling the heater and condition actuators. */
  trait Thermostat extends Consumer[TemperatureEntry] with State[TemperatureEntry]

  object Thermostat:

    def apply(): Thermostat = ThermostatImpl()

    private class ThermostatImpl extends Thermostat:
      override protected def react(e: Try[TemperatureEntry]): Unit =
        println(s"[THERMOSTAT] Received $e")
