package io.github.tassiLuca.hubkt.core.ports

import io.github.tassiLuca.hubkt.core.Temperature

/** The dashboard service. */
interface DashboardService {

    /** Updates the temperature. */
    fun temperatureUpdated(newTemperature: Temperature)

    /** Updates the luminosity. */
    fun onHeaterNotified()

    /** Notifies heater has been turned off. */
    fun offHeaterNotified()

    /** Notifies alert. */
    fun alertNotified(msg: String)
}
