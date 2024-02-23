package io.github.tassiLuca.hub.infrastructure.ui

import io.github.tassiLuca.utils.ScalaSwingFacade.{*, given}

import java.awt.{BorderLayout, Font, GridLayout}
import javax.swing.*

class DashboardUI extends JFrame:
  private val biggerFont = Font("Arial", Font.BOLD, 20)

  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setTitle("Smart hub dashboard")
  setSize(800, 200)
  setLocationRelativeTo(null)
  setLayout(GridLayout(2, 1))
  val temperatureLabel: JLabel = JLabel("00°C")
  temperatureLabel.setFont(biggerFont)
  val heaterLabel: JLabel = JLabel("Off")
  heaterLabel.setFont(biggerFont)
  val alertTextArea: JTextArea = JTextArea(5, 65)
  alertTextArea.setEditable(false)
  private val temperatureInfos = createPanel(
    createPanel(JLabel("Current Temperature:"), temperatureLabel),
    createPanel(JLabel("Heater Status:"), heaterLabel),
  )
  private val alerts = createPanel(
    (JLabel("Alerts:"), BorderLayout.NORTH),
    (JScrollPane(alertTextArea), BorderLayout.CENTER),
  )
  getContentPane.add(temperatureInfos)
  getContentPane.add(alerts)
  setVisible(true)
