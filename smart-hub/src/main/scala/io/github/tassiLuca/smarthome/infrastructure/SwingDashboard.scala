package io.github.tassiLuca.smarthome.infrastructure

import io.github.tassiLuca.smarthome.core.{DashboardComponent, TemperatureEntry}

import java.awt.{BorderLayout, FlowLayout, Font, GridLayout}
import javax.swing.{JFrame, JLabel, JPanel, JScrollPane, JTextArea, SwingUtilities, WindowConstants}

trait SwingDashboard extends DashboardComponent:
  override val dashboard: Dashboard = new Dashboard:

    private val view: MainFrame = MainFrame()

    override def updateTemperature(entries: Seq[TemperatureEntry]): Unit = SwingUtilities.invokeLater { () =>
      view.temperatureLabel.setText("NEW TEMP")
    }

    override def newHeaterState(state: HeaterState): Unit = SwingUtilities.invokeLater { () =>
      view.heaterLabel.setText(state.toString)
    }

    override def newAlert(msg: String): Unit = SwingUtilities.invokeLater { () =>
      view.alertTextArea.setText(msg)
    }

    private class MainFrame extends JFrame:
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      setSize(300, 200)
      setLayout(new GridLayout(3, 1))
      val temperatureLabel = new JLabel("00°C")
      temperatureLabel.setFont(new Font("Arial", Font.BOLD, 20))
      val heaterLabel = new JLabel("Off")
      heaterLabel.setFont(new Font("Arial", Font.BOLD, 20))
      val alertTextArea = new JTextArea(5, 20)
      alertTextArea.setEditable(false)
      val tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
      tempPanel.add(new JLabel("Current Temperature:"))
      tempPanel.add(temperatureLabel)
      val heaterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
      heaterPanel.add(new JLabel("Heater Status:"))
      heaterPanel.add(heaterLabel)
      val alertPanel = new JPanel(new BorderLayout())
      alertPanel.add(new JLabel("Alerts:"), BorderLayout.NORTH)
      alertPanel.add(new JScrollPane(alertTextArea), BorderLayout.CENTER)
      add(tempPanel)
      add(heaterPanel)
      add(alertPanel)
      setVisible(true)
