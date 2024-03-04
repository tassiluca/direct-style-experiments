package io.github.tassiLuca.hub.adapters

import io.github.tassiLuca.hub.application.LightingManager
import io.github.tassiLuca.hub.application.ThermostatManager
import io.github.tassiLuca.hub.core.LuminosityEntry
import io.github.tassiLuca.hub.core.TemperatureEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/** A mocked hub manager. */
class MockedHubManager(override val coroutineContext: CoroutineContext) : CoroutineScope {

    /** The dashboard. */
    val dashboard = SwingDashboardService()

    /** The thermostat manager. */
    val thermostatManager = ThermostatManager(dashboard, coroutineContext)

    /** The lighting system manager. */
    val lightingManager = LightingManager(dashboard, coroutineContext)

    /** The sensor source. */
    val sensorSource = GraphicalSource()

    /** Runs the mocked hub manager. */
    suspend fun run() = coroutineScope {
        val temperatureFlow = sensorSource.sensorEvents
            .filter { it is TemperatureEntry }
            .map { it as TemperatureEntry }
        val luminosityFlow = sensorSource.sensorEvents
            .filter { it is LuminosityEntry }
            .map { it as LuminosityEntry }
        launch { thermostatManager.run(temperatureFlow) }
        lightingManager.run(luminosityFlow)
    }
}
