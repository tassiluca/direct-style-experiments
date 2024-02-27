package io.github.tassiLuca.hub.adapters.ui

import io.github.tassiLuca.utils.ScalaSwingFacade.{*, given}

import java.awt.{BorderLayout, Font, GridLayout}
import javax.swing.*
import javax.swing.table.DefaultTableModel

class DashboardUI extends JFrame:
  private val biggerFont = Font("Arial", Font.BOLD, 20)

  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setTitle("Smart hub dashboard")
  setSize(915, 460)
  setLocationRelativeTo(null)
  setLayout(GridLayout(1, 2))
  val temperatureLabel: JLabel = JLabel("--")
  temperatureLabel.setFont(biggerFont)
  val heaterLabel: JLabel = JLabel("--")
  heaterLabel.setFont(biggerFont)
  val luminosityModel: DefaultListModel[String] = DefaultListModel[String]()
  private val luminosityEntries = JList(luminosityModel)
  private val infos = createPanel(
    JSeparator(),
    createPanel(JLabel("Current Temperature:"), temperatureLabel),
    createPanel(JLabel("Heater Status:"), heaterLabel),
    createPanel(JLabel("Current Luminosity:"), luminosityEntries),
  )
  infos.setLayout(BoxLayout(infos, BoxLayout.PAGE_AXIS))
  val alertsModel: DefaultTableModel = DefaultTableModel(Array[AnyRef]("Alert Messages"), 0)
  private val alertsLog: JTable = JTable(alertsModel)
  private val alerts = createPanel(JScrollPane(alertsLog))
  alerts.setLayout(BoxLayout(alerts, BoxLayout.PAGE_AXIS))
  getContentPane.add(infos)
  getContentPane.add(alerts)
  setVisible(true)
