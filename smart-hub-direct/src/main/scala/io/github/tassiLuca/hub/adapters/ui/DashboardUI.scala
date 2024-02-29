package io.github.tassiLuca.hub.adapters.ui

import io.github.tassiLuca.pimping.ScalaSwingFacade.createPanel
import io.github.tassiLuca.pimping.ScalaSwingFacade.given 

import java.awt.{Dimension, Font, GridLayout}
import javax.swing.*
import javax.swing.table.DefaultTableModel

class DashboardUI extends JFrame:
  private val biggerFont = Font("Arial", Font.BOLD, 20)

  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setTitle("Smart hub dashboard")
  setSize(950, 460)
  setLocationRelativeTo(null)
  setLayout(GridLayout(1, 2))
  val temperatureLabel: JLabel = JLabel("--")
  temperatureLabel.setFont(biggerFont)
  val heaterLabel: JLabel = JLabel("Off")
  heaterLabel.setFont(biggerFont)
  val luminosityLabel: JLabel = JLabel("--")
  luminosityLabel.setFont(biggerFont)
  val scheduleModel: DefaultTableModel = DefaultTableModel(Array[AnyRef]("Day", "Timespan", "Target temperature"), 0)
  private val scheduleTable = JScrollPane(JTable(scheduleModel))
  scheduleTable.setPreferredSize(Dimension(475, 150))
  private val infos = createPanel(
    createPanel(JLabel("Schedule"), scheduleTable),
    createPanel(JLabel("Current Temperature:"), temperatureLabel),
    createPanel(JLabel("Heater Status:"), heaterLabel),
    createPanel(JLabel("Current Luminosity:"), luminosityLabel),
  )
  infos.setLayout(BoxLayout(infos, BoxLayout.PAGE_AXIS))
  val alertsModel: DefaultTableModel = DefaultTableModel(Array[AnyRef]("Alert Messages"), 0)
  private val alertsLog: JTable = JTable(alertsModel)
  private val alerts = createPanel(JScrollPane(alertsLog))
  alerts.setLayout(BoxLayout(alerts, BoxLayout.PAGE_AXIS))
  getContentPane.add(infos)
  getContentPane.add(alerts)
  setVisible(true)
