package io.github.tassiLuca.hub.core

import io.github.tassiLuca.hub.core.ports.DashboardService
import java.util.Date
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/** A sensor health checker. */
class SensorHealthChecker<in E : SensorEvent>(
    override val samplingWindow: Duration,
    override val coroutineContext: CoroutineContext,
    private val dashboardService: DashboardService,
) : ScheduledConsumer<E, List<String>> {

    private var activeSensors = emptySet<String>()

    @get:Synchronized @set:Synchronized
    override var state: List<String> = emptyList()

    override suspend fun react(e: E) {
        state = state + e.sourceUnit
    }

    override suspend fun update() {
        if (state.isNotEmpty()) {
            if (activeSensors.isNotEmpty() && activeSensors != state.toSet()) {
                val noMoreActive = activeSensors - state.toSet()
                dashboardService.alertNotified(
                    "[${Date()}] Detected ${noMoreActive.joinToString(", ")} no more active!",
                )
            }
            activeSensors = state.toSet()
            state = emptyList()
        }
    }
}
