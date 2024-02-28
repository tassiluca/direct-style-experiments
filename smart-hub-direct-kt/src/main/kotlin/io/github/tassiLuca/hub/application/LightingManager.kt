package io.github.tassiLuca.hub.application

import io.github.tassiLuca.hub.core.LightingSystem
import io.github.tassiLuca.hub.core.LuminosityEntry
import io.github.tassiLuca.hub.core.ports.DashboardService
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

/** Manages the thermostat and the temperature sensors. */
class LightingManager(dashboardService: DashboardService, coroutineContext: CoroutineContext) {

    /** The thermostat. */
    val lightingSystem = LightingSystem(SAMPLING_WINDOW, coroutineContext, dashboardService)

    /** Runs the thermostat and the temperature sensors. */
    suspend fun run(sensorSource: Flow<LuminosityEntry>) {
        println("LightingManager: run")
        lightingSystem.run()
        sensorSource.collect {
            println("LightingManager: $it")
            lightingSystem.react(it)
        }
    }

    companion object {
        private val SAMPLING_WINDOW = 5.seconds
    }
}
