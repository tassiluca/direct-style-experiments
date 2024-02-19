package io.github.tassiLuca.smarthome.coroutines.infrastructure

import io.github.tassiLuca.smarthome.coroutines.core.SensorEvent
import io.github.tassiLuca.smarthome.coroutines.core.SensorSource
import io.github.tassiLuca.smarthome.coroutines.core.TemperatureEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import kotlin.coroutines.CoroutineContext

/** A graphical temperature source. */
class GraphicalTemperatureSource : SensorSource<SensorEvent>, CoroutineScope {

    private val entries = MutableSharedFlow<SensorEvent>()
    override val sensorEvents: SharedFlow<SensorEvent> = entries.asSharedFlow()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    init {
        SensorApp()
    }

    private fun publishEntry(entry: SensorEvent) = launch {
        println("[${Thread.currentThread().name}] Publishing entry: $entry")
        entries.emit(TemperatureEntry(entry.sourceUnit, 0.0))
    }

    private inner class SensorApp : JFrame() {
        private val sensors = mutableListOf<String>()
        private val addSensorButton = JButton("Add New Sensor")
        private val sensorsPanel = JPanel()

        init {
            addSensorButton.addActionListener { addSensor() }
            val contentPane = contentPane
            contentPane.layout = BorderLayout()
            val topPanel = JPanel()
            topPanel.add(addSensorButton)
            contentPane.add(topPanel, BorderLayout.NORTH)
            sensorsPanel.layout = BoxLayout(sensorsPanel, BoxLayout.Y_AXIS)
            contentPane.add(sensorsPanel, BorderLayout.CENTER)
            pack()
            setSize(GUI_WIDTH, GUI_HEIGHT)
            setLocationRelativeTo(null)
            defaultCloseOperation = EXIT_ON_CLOSE
            isVisible = true
        }

        private fun addSensor() {
            val sensor = "Sensor ${sensors.size + 1}"
            sensors.add(sensor)
            sensorsPanel.add(sensorPanel(sensor))
            sensorsPanel.revalidate()
            sensorsPanel.repaint()
        }

        private fun sensorPanel(name: String): JPanel {
            val panel = JPanel()
            panel.layout = FlowLayout()
            panel.add(JLabel(name))
            panel.add(JTextField(4))
            JButton("Send").apply {
                addActionListener {
                    publishEntry(TemperatureEntry(name, 0.0))
                }
            }.also { panel.add(it) }
            return panel
        }
    }

    companion object {
        private const val GUI_WIDTH = 400
        private const val GUI_HEIGHT = 300
    }
}
