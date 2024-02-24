package io.github.tassiLuca.hub.adapters.ui

import io.github.tassiLuca.utils.ScalaSwingFacade.{*, given}

import java.awt.{Font, GridLayout}
import javax.swing.*
import javax.swing.table.DefaultTableModel

class DashboardUI extends JFrame:
  private val biggerFont = Font("Arial", Font.BOLD, 20)

  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setTitle("Smart hub dashboard")
  setSize(915, 460)
  setLocationRelativeTo(null)
  setLayout(GridLayout(1, 2))
  val temperatureLabel: JLabel = JLabel("00°C")
  temperatureLabel.setFont(biggerFont)
  val heaterLabel: JLabel = JLabel("Off")
  heaterLabel.setFont(biggerFont)
  val luminosityLabel: JLabel = JLabel("--")
  luminosityLabel.setFont(biggerFont)
  private val infos = createPanel(
    createPanel(),
    createPanel(JLabel("Current Temperature:"), temperatureLabel),
    createPanel(JLabel("Heater Status:"), heaterLabel),
    createPanel(JLabel("Current Luminosity:"), luminosityLabel),
  )
  infos.setLayout(BoxLayout(infos, BoxLayout.PAGE_AXIS))
  val alertsModel: DefaultTableModel = DefaultTableModel(Array[AnyRef]("Alert Messages"), 0)
  private val alertsLog: JTable = JTable(alertsModel)
  private val alerts = createPanel(JScrollPane(alertsLog))
  getContentPane.add(infos)
  getContentPane.add(alerts)
  setVisible(true)
