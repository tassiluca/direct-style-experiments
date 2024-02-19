package io.github.tassiLuca.smarthome.coroutines.core

/** A sensor health checker. */
class SensorHealthChecker<in E : SensorEvent> : SensorSourceConsumer<E, List<String>> {

    override var state: List<String> = emptyList()

    override suspend fun react(e: E) {
        println("Sensor health checker reacts to event from ${e.sourceUnit}")
    }
}
