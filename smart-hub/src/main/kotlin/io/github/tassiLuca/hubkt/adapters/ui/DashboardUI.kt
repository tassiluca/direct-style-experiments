package io.github.tassiLuca.hubkt.adapters.ui

import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.WindowConstants
import javax.swing.table.DefaultTableModel

/** The dashboard Swing UI. */
class DashboardUI : JFrame() {

    /** The temperature label. */
    val temperatureLabel = JLabel("--")

    /** The heater label. */
    val heaterLabel = JLabel("--")

    /** The alerts model. */
    val alertsModel = DefaultTableModel(arrayOf<Any>("Alert Messages"), 0)

    private val biggerFont = Font("Arial", Font.BOLD, BIGGER_FONT_SIZE)

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        title = "Smart hub dashboard"
        setSize(WIDTH, HEIGHT)
        setLocationRelativeTo(null)
        layout = GridLayout(1, 2)
        temperatureLabel.font = biggerFont
        heaterLabel.font = biggerFont
        val luminosityLabel = JLabel("--")
        luminosityLabel.font = biggerFont
        val infos = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
            add(JPanel())
            add(
                JPanel().apply {
                    add(JLabel("Current Temperature:"))
                    add(temperatureLabel)
                },
            )
            add(
                JPanel().apply {
                    add(JLabel("Heater Status:"))
                    add(heaterLabel)
                },
            )
            add(
                JPanel().apply {
                    add(JLabel("Current Luminosity:"))
                    add(luminosityLabel)
                },
            )
        }
        val alerts = JPanel(BorderLayout()).apply {
            add(JScrollPane(JTable(alertsModel)))
        }
        contentPane.add(infos)
        contentPane.add(alerts)
        isVisible = true
    }

    companion object {
        private const val WIDTH = 915
        private const val HEIGHT = 460
        private const val BIGGER_FONT_SIZE = 20
    }
}
