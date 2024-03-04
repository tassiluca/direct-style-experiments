package io.github.tassiLuca.hub.application

import io.github.tassiLuca.hub.core.SensorHealthChecker
import io.github.tassiLuca.hub.core.TemperatureEntry
import io.github.tassiLuca.hub.core.Thermostat
import io.github.tassiLuca.hub.core.ports.DashboardService
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

/** Manages the thermostat and the temperature sensors. */
class ThermostatManager(private val dashboardService: DashboardService, coroutineContext: CoroutineContext) {

    /** The thermostat. */
    val thermostat = Thermostat(TEMPERATURE_TARGET, SAMPLING_WINDOW, dashboardService, coroutineContext)

    /** The temperature sensors checker. */
    val temperatureSensorsChecker = SensorHealthChecker<TemperatureEntry>(
        SAMPLING_WINDOW,
        coroutineContext,
        dashboardService,
    )

    /** Runs the thermostat and the temperature sensors. */
    suspend fun run(sensorSource: Flow<TemperatureEntry>) {
        dashboardService.updateSchedule(mapOf("ALWAYS" to "09-24" to "19.0"))
        thermostat.run()
        temperatureSensorsChecker.run()
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
