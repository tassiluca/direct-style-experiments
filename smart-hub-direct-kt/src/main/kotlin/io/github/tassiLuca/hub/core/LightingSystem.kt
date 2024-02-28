package io.github.tassiLuca.hub.core

import io.github.tassiLuca.hub.core.ports.DashboardService
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/** A sensor health checker. */
class LightingSystem(
    override val period: Duration,
    override val coroutineContext: CoroutineContext,
    private val dashboardService: DashboardService,
) : ScheduledConsumer<LuminosityEntry, List<LuminosityEntry>> {

    @get:Synchronized @set:Synchronized
    override var state: List<LuminosityEntry> = emptyList()

    override suspend fun react(e: LuminosityEntry) {
        state = state + e
    }

    override suspend fun update() {
        println("LightingSystem: $state")
        if (state.isNotEmpty()) {
            val average = state.map { it.luminosity }.average()
            dashboardService.luminosityUpdated(average)
            state = emptyList()
        }
    }
}
