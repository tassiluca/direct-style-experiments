package io.github.tassiLuca.hub.coroutines.application

import io.github.tassiLuca.hub.coroutines.core.SensorHealthChecker
import io.github.tassiLuca.hub.coroutines.core.TemperatureEntry
import io.github.tassiLuca.hub.coroutines.core.Thermostat
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

/** Manages the thermostat and the temperature sensors. */
class ThermostatHubManager(coroutineContext: CoroutineContext) {

    /** The thermostat. */
    val thermostat = Thermostat(10.seconds, coroutineContext)

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
}
