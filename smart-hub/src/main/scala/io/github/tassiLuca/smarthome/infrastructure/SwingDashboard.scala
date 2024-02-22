package io.github.tassiLuca.smarthome.infrastructure

import io.github.tassiLuca.smarthome.core.{DashboardComponent, Temperature}
import io.github.tassiLuca.smarthome.infrastructure.ui.DashboardUI

import javax.swing.SwingUtilities

trait SwingDashboard extends DashboardComponent:
  override val dashboard: Dashboard = new Dashboard:

    private val view = DashboardUI()

    override def temperatureUpdated(temperature: Temperature): Unit = SwingUtilities.invokeLater { () =>
      view.temperatureLabel.setText(s"$temperature °C")
    }

    override def offHeaterNotified(): Unit = SwingUtilities.invokeLater { () =>
      view.heaterLabel.setText("Off")
    }
    
    override def onHeaterNotified(): Unit = SwingUtilities.invokeLater { () =>
      view.heaterLabel.setText("On")
    }
    
    override def alertNotified(msg: String): Unit = SwingUtilities.invokeLater { () =>
      view.alertTextArea.setText(msg)
    }
