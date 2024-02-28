package io.github.tassiLuca.hub.core.ports

import io.github.tassiLuca.hub.core.{Luminosity, LuminosityEntry, Temperature}

/** The component encapsulating the dashboard port. */
trait DashboardServiceComponent:

  /** The [[DashboardService]] instance. */
  val dashboard: DashboardService

  /** The dashboard service port through which is possible notify the dashboard state changes. */
  trait DashboardService:
    /** Notifies the [[luminosity]] entries have changed. */
    def luminosityUpdate(luminosity: Luminosity): Unit

    /** Notifies the [[temperature]] has changed. */
    def temperatureUpdated(temperature: Temperature): Unit

    /** Notifies the heater has been turned on. */
    def onHeaterNotified(): Unit

    /** Notifies the heater has been turned off. */
    def offHeaterNotified(): Unit

    /** Notifies an alert [[message]] has been arisen. */
    def alertNotified(message: String): Unit

    /** Notifies the [[schedule]] has been updated. */
    def updateSchedule(schedule: Map[(String, String), String]): Unit
