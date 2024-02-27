package io.github.tassiLuca.hub.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** A consumer of sensor events. */
interface SensorSourceConsumer<in E : SensorEvent, out S> {

    /** The current state of the source consumer. */
    val state: S

    /** Reacts to a sensor event. */
    suspend fun react(e: E)
}

/** A scheduled consumer of sensor events. */
interface ScheduledConsumer<in E : SensorEvent, out S> : SensorSourceConsumer<E, S>, CoroutineScope {

    /** The interval period. */
    val period: Duration

    /** The update logic of the consumer. */
    suspend fun update()

    /** Runs the consumer scheduler. */
    fun run() = launch {
        while (true) {
            update()
            delay(period)
        }
    }
}
