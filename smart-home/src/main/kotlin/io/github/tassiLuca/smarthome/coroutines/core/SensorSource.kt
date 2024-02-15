package io.github.tassiLuca.smarthome.coroutines.core

import kotlinx.coroutines.flow.SharedFlow

/** A consumer of sensor events. */
interface SensorSourceConsumer<in E : SensorEvent> {
    /** Reacts to a sensor event. */
    suspend fun react(e: E)
}

/** A source of sensor events. */
interface SensorSource<out E : SensorEvent> {
    /** The flow of sensor events. */
    val sensorEvents: SharedFlow<E>
}

/** A detection performed by a sensing unit. */
sealed interface SensorEvent {
    /** The identifier of the sensing unit that generated the event. */
    val sourceUnit: String
}

/** A temperature detection. */
data class TemperatureEntry(
    override val sourceUnit: String,
    /** The temperature value in Celsius degrees. */
    val temperature: Double,
) : SensorEvent

/** A luminosity detection. */
data class LuminosityEntry(
    override val sourceUnit: String,
    /** The luminosity value in lux. */
    val luminosity: Double,
) : SensorEvent
