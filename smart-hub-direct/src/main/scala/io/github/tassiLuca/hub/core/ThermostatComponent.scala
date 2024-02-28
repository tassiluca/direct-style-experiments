package io.github.tassiLuca.hub.core

import gears.async.Async
import io.github.tassiLuca.hub.core.ports.{DashboardServiceComponent, HeaterComponent}
import io.github.tassiLuca.rears.{Consumer, State}

import scala.util.Try

/** The component encapsulating the [[Thermostat]] entity. */
trait ThermostatComponent[T <: ThermostatScheduler]:
  context: HeaterComponent & DashboardServiceComponent =>

  /** The [[Thermostat]] instance. */
  val thermostat: Thermostat

  /** A [[state]]ful consumer of [[TemperatureEntry]]s in charge of controlling
    * the heater and keeping track of the last detection average temperature.
    */
  trait Thermostat
      extends Consumer[Seq[TemperatureEntry], Option[Temperature]]
      with State[Seq[TemperatureEntry], Option[Temperature]]:
    val scheduler: T

  object Thermostat:
    /** Creates a [[Thermostat]] with the given [[thermostatScheduler]]. */
    def apply(scheduler: T): Thermostat = ThermostatImpl(scheduler)

    private class ThermostatImpl(override val scheduler: T)
        extends Thermostat
        with State[Seq[TemperatureEntry], Option[Temperature]](None):

      private val hysteresis = 1.5

      override protected def react(e: Try[Seq[TemperatureEntry]])(using Async): Option[Temperature] =
        for
          averageTemperature <- e.map { entries => entries.map(_.temperature).sum / entries.size }.toOption
          _ = averageTemperature.evaluate()
        yield averageTemperature

      extension (t: Temperature)
        private def evaluate()(using Async): Unit =
          context.dashboard.temperatureUpdated(t)
          val target = scheduler.currentTarget
          if t > target + hysteresis then offHeater() else if t < target then onHeater()

        private def offHeater()(using Async): Unit =
          context.heater.off()
          context.dashboard.offHeaterNotified()

        private def onHeater()(using Async): Unit =
          context.heater.on()
          context.dashboard.onHeaterNotified()
