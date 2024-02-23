package io.github.tassiLuca.hub.core

import gears.async.Async
import io.github.tassiLuca.hub.core.ports.{DashboardServiceComponent, HeaterComponent}

import scala.util.Try
import io.github.tassiLuca.rears.{Consumer, State}

/** The component encapsulating the thermostat. */
trait ThermostatComponent:
  context: HeaterComponent & DashboardServiceComponent =>

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
        for
          averageTemperature <- e.map { entries => entries.map(_.temperature).sum / entries.size }.toOption
          _ = averageTemperature.evaluate()
        yield averageTemperature

      extension (t: Temperature)
        private def evaluate()(using Async): Unit =
          val target = scheduler.currentTarget
          context.dashboard.temperatureUpdated(t)
          if t > target + hysteresis then offHeater() else if t < target then onHeater()

        private def offHeater()(using Async): Unit =
          context.heater.off(); context.dashboard.offHeaterNotified()

        private def onHeater()(using Async): Unit =
          context.heater.on(); context.dashboard.onHeaterNotified()
