package io.github.tassiLuca.smarthome.coroutines.core

/** A sensor health checker. */
class SensorHealthChecker<in E : SensorEvent> : SensorSourceConsumer<E> {
    override suspend fun react(e: E) {
        TODO("Not yet implemented")
    }
}
