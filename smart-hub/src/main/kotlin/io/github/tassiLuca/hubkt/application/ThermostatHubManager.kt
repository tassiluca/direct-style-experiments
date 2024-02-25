package io.github.tassiLuca.hubkt.application

import io.github.tassiLuca.hubkt.core.SensorHealthChecker
import io.github.tassiLuca.hubkt.core.TemperatureEntry
import io.github.tassiLuca.hubkt.core.Thermostat
import io.github.tassiLuca.hubkt.core.ports.DashboardService
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

/** Manages the thermostat and the temperature sensors. */
class ThermostatHubManager(dashboardService: DashboardService, coroutineContext: CoroutineContext) {

    /** The thermostat. */
    val thermostat = Thermostat(TEMPERATURE_TARGET, SAMPLING_WINDOW, dashboardService, coroutineContext)

    /** The temperature sensors checker. */
    val temperatureSensorsChecker = SensorHealthChecker<TemperatureEntry>()

    /** Runs the thermostat and the temperature sensors. */
    suspend fun run(sensorSource: Flow<TemperatureEntry>) {
        thermostat.run()
        sensorSource.collect {
            thermostat.react(it)
            temperatureSensorsChecker.react(it)
        }
    }

    companion object {
        private const val TEMPERATURE_TARGET = 19.0
        private val SAMPLING_WINDOW = 10.seconds
    }
}
