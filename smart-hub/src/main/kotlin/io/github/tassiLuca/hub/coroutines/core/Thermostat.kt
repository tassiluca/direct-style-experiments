package io.github.tassiLuca.hub.coroutines.core

import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/** A thermostat. */
class Thermostat(
    override val period: Duration,
    override val coroutineContext: CoroutineContext,
) : ScheduledConsumer<TemperatureEntry, List<TemperatureEntry>> {

    @get:Synchronized @set:Synchronized
    override var state: List<TemperatureEntry> = emptyList()

    override suspend fun react(e: TemperatureEntry) {
        println("Thermostat reacts to temperature ${e.temperature} from ${e.sourceUnit}")
        state = state + e
    }

    override suspend fun update() {
        println("Thermostat updates its state: $state")
        state = emptyList()
    }
}
