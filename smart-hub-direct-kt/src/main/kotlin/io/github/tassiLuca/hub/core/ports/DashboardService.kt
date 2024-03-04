package io.github.tassiLuca.hub.core.ports

import io.github.tassiLuca.hub.core.Luminosity
import io.github.tassiLuca.hub.core.Temperature

/** The dashboard service. */
interface DashboardService {

    /** Updates the temperature with [newTemperature]. */
    fun temperatureUpdated(newTemperature: Temperature)

    /** Updates the luminosity with [newLuminosity]. */
    fun luminosityUpdated(newLuminosity: Luminosity)

    /** Updates the luminosity. */
    fun onHeaterNotified()

    /** Notifies heater has been turned off. */
    fun offHeaterNotified()

    /** Notifies an alert [message]. */
    fun alertNotified(message: String)

    /** Updates the [schedule]. */
    fun updateSchedule(schedule: Map<Pair<String, String>, String>)
}
