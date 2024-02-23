package io.github.tassiLuca.hub.core.ports

import io.github.tassiLuca.hub.core.Temperature

/** The component encapsulating the dashboard. */
trait DashboardServiceComponent:

  /** The [[DashboardService]] instance. */
  val dashboard: DashboardService

  /** The dashboard boundary. */
  trait DashboardService:
    def temperatureUpdated(newTemperature: Temperature): Unit
    def onHeaterNotified(): Unit
    def offHeaterNotified(): Unit
    def alertNotified(msg: String): Unit
