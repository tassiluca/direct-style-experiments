package io.github.tassiLuca.hub.adapters

import io.github.tassiLuca.hub.adapters.ui.TemperatureSourceUI
import io.github.tassiLuca.hub.core.SensorEvent
import io.github.tassiLuca.hub.core.SensorSource
import io.github.tassiLuca.hub.core.TemperatureEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/** A graphical temperature source. */
class GraphicalTemperatureSource : SensorSource<SensorEvent>, CoroutineScope {

    private val entries = MutableSharedFlow<SensorEvent>()
    override val sensorEvents: SharedFlow<SensorEvent> = entries.asSharedFlow()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    init {
        val view = TemperatureSourceUI { (s, d) -> publishEntry(s, d) }
        view.pack()
        view.isVisible = true
    }

    private fun publishEntry(sensorName: String, value: Double) {
        launch {
            println("[${Thread.currentThread().name}] Publishing entry: $sensorName -> $value")
            entries.emit(TemperatureEntry(sensorName, value))
        }
    }
}
