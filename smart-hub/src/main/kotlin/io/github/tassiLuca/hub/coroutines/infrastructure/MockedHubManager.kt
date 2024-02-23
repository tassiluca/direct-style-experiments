package io.github.tassiLuca.hub.coroutines.infrastructure

import io.github.tassiLuca.hub.coroutines.application.ThermostatHubManager
import io.github.tassiLuca.hub.coroutines.core.TemperatureEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext

/** A mocked hub manager. */
class MockedHubManager(coroutineContext: CoroutineContext) {

    /** The thermostat hub. */
    val thermostatHub = ThermostatHubManager(coroutineContext)

    /** The sensor source. */
    val sensorSource = GraphicalTemperatureSource()

    /** Runs the mocked hub manager. */
    suspend fun run() {
        val temperatureFlow: Flow<TemperatureEntry> = sensorSource.sensorEvents
            .filter { it is TemperatureEntry }
            .map { it as TemperatureEntry }
        thermostatHub.run(temperatureFlow)
    }
}
