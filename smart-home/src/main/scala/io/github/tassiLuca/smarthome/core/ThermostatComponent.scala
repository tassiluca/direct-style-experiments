package io.github.tassiLuca.smarthome.core

import gears.async.Async

import scala.util.Try
import io.github.tassiLuca.rears.{Consumer, State}

trait ThermostatComponent:
  context: ThermostatSchedulerComponent with HVACControllerComponent =>

  /** The [[Thermostat]] instance. */
  val thermostat: Thermostat

  /** The entity in charge of controlling the heater and conditioner actuators based on read [[TemperatureEntry]]s. */
  trait Thermostat extends Consumer[Seq[TemperatureEntry]] with State[Seq[TemperatureEntry]]

  object Thermostat:
    def apply(): Thermostat = ThermostatImpl()

    private class ThermostatImpl extends Thermostat:
      override protected def react(e: Try[Seq[TemperatureEntry]])(using Async): Unit =
        println(s"[THERMOSTAT] Received $e")
