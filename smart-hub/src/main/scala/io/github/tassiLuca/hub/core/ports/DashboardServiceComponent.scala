package io.github.tassiLuca.hub.core.ports

import io.github.tassiLuca.hub.core.{Luminosity, LuminosityEntry, Temperature}

/** The component encapsulating the dashboard. */
trait DashboardServiceComponent:

  /** The [[DashboardService]] instance. */
  val dashboard: DashboardService

  /** The dashboard boundary. */
  trait DashboardService:
    def luminosityUpdate(luminosity: Seq[LuminosityEntry]): Unit
    def temperatureUpdated(newTemperature: Temperature): Unit
    def onHeaterNotified(): Unit
    def offHeaterNotified(): Unit
    def alertNotified(msg: String): Unit
