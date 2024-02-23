package io.github.tassiLuca.hub.core

import gears.async.Async

import scala.util.Try
import io.github.tassiLuca.rears.{Consumer, State}

trait ThermostatComponent:
  context: HeaterComponent & DashboardComponent =>

  /** The [[Thermostat]] instance. */
  val thermostat: Thermostat

  /** The entity in charge of controlling the heater and conditioner actuators based on read [[TemperatureEntry]]s. */
  trait Thermostat
      extends Consumer[Seq[TemperatureEntry], Option[Temperature]]
      with State[Seq[TemperatureEntry], Option[Temperature]]:
    val scheduler: ThermostatScheduler

  object Thermostat:
    def apply(thermostatScheduler: ThermostatScheduler): Thermostat = ThermostatImpl(thermostatScheduler)

    private class ThermostatImpl(override val scheduler: ThermostatScheduler) extends Thermostat:

      private val hysteresis = 1.5

      override protected def react(e: Try[Seq[TemperatureEntry]])(using Async): Option[Temperature] =
        e.map { entries => entries.map(_.temperature).sum / entries.size }
          .map { avg => avg.evaluate(); avg }
          .toOption

      extension (t: Temperature)
        private def evaluate()(using Async): Unit =
          val target = scheduler.currentTarget
          context.dashboard.temperatureUpdated(t)
          if t > target + hysteresis then offHeater() else if t < target then onHeater()

        private def offHeater()(using Async): Unit =
          context.heater.off(); context.dashboard.offHeaterNotified()

        private def onHeater()(using Async): Unit =
          context.heater.on(); context.dashboard.onHeaterNotified()
