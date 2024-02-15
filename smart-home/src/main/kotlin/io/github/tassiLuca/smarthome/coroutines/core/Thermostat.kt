package io.github.tassiLuca.smarthome.coroutines.core

/** A thermostat. */
class Thermostat : SensorSourceConsumer<TemperatureEntry> {
    override suspend fun react(e: TemperatureEntry) {
        TODO("Not yet implemented")
    }
}
