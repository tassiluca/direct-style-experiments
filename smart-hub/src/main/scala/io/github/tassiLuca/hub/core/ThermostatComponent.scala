package io.github.tassiLuca.hub.core

import gears.async.Async

import scala.util.Try
import io.github.tassiLuca.rears.{Consumer, State}

trait ThermostatComponent:
  context: HeaterComponent & DashboardComponent =>

  /** The [[Thermostat]] instance. */
  val thermostat: Thermostat

  /** The entity in charge of controlling the heater and conditioner actuators based on read [[TemperatureEntry]]s. */
  trait Thermostat extends Consumer[Seq[TemperatureEntry]] with State[Seq[TemperatureEntry]]:
    val scheduler: ThermostatScheduler

  object Thermostat:
    def apply(thermostatScheduler: ThermostatScheduler): Thermostat = ThermostatImpl(thermostatScheduler)

    private class ThermostatImpl(override val scheduler: ThermostatScheduler) extends Thermostat:

      private val hysteresis = 1.5

      override protected def react(e: Try[Seq[TemperatureEntry]])(using Async): Unit =
        for
          entries <- e
          average = entries.map(_.temperature).sum / entries.size
        yield average.evaluate()

      extension (t: Temperature)
        private def evaluate()(using Async): Unit =
          context.dashboard.temperatureUpdated(t)
          if t > scheduler.currentTarget + hysteresis then offHeater() else onHeater()

        private def offHeater()(using Async): Unit =
          context.heater.off(); context.dashboard.offHeaterNotified()

        private def onHeater()(using Async): Unit =
          context.heater.on(); context.dashboard.onHeaterNotified()
