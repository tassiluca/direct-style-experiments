package io.github.tassiLuca.hub.adapters

import io.github.tassiLuca.hub.adapters.ui.SourceUI
import io.github.tassiLuca.hub.core.Luminosity
import io.github.tassiLuca.hub.core.LuminosityEntry
import io.github.tassiLuca.hub.core.SensorEvent
import io.github.tassiLuca.hub.core.SensorSource
import io.github.tassiLuca.hub.core.Temperature
import io.github.tassiLuca.hub.core.TemperatureEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import scala.runtime.BoxedUnit
import kotlin.coroutines.CoroutineContext

/** A graphical temperature source. */
class GraphicalSource : SensorSource<SensorEvent>, CoroutineScope {

    private val views = setOf(
        SourceUI("temperature") { s, d ->
            publishTemperatureEntry(s, d as Double)
            BoxedUnit.UNIT
        },
        SourceUI("luminosity") { s, d ->
            publishLuminosityEntry(s, d as Double)
            BoxedUnit.UNIT
        },
    )
    private val entries = MutableSharedFlow<SensorEvent>()

    override val sensorEvents: SharedFlow<SensorEvent> = entries.asSharedFlow()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    init {
        views.forEach {
            it.pack()
            it.isVisible = true
        }
    }

    private fun publishTemperatureEntry(sensorName: String, temperature: Temperature) = launch {
        entries.emit(TemperatureEntry(sensorName, temperature))
    }

    private fun publishLuminosityEntry(sensorName: String, luminosity: Luminosity) = launch {
        entries.emit(LuminosityEntry(sensorName, luminosity))
    }
}
