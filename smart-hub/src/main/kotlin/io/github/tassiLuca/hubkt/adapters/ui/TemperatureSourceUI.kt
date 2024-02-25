package io.github.tassiLuca.hubkt.adapters.ui

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.WindowConstants

/** A simulated temperature source in a GUI. */
class TemperatureSourceUI(private val publishHandler: (Pair<String, Double>) -> Unit) : JFrame() {

    private var sensors = 0

    init {
        title = "A simulated temperature source in a GUI pretending to be real"
        preferredSize = Dimension(WIDTH, HEIGHT)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        val middle = JPanel()
        middle.layout = BoxLayout(middle, BoxLayout.Y_AXIS)
        val addButton = JButton("Add Sensor")
        addButton.addActionListener { addSensorTo(middle) }
        val mainPanel = JPanel(BorderLayout()).apply {
            add(middle, BorderLayout.CENTER)
            add(addButton, BorderLayout.SOUTH)
        }
        contentPane.add(mainPanel)
    }

    private fun addSensorTo(panel: JPanel): Component {
        sensors++
        val sendButton = JButton("Send")
        val sensorName = "sensor-$sensors"
        val sensorInput = JTextField(4)
        sendButton.addActionListener { publishHandler(Pair(sensorName, sensorInput.text.toDouble())) }
        JPanel().apply {
            add(JLabel(sensorName))
            add(sensorInput)
            add(sendButton)
        }.also { panel.add(it) }
        panel.revalidate()
        panel.repaint()
        return panel
    }

    companion object {
        private const val WIDTH = 500
        private const val HEIGHT = 300
    }
}
