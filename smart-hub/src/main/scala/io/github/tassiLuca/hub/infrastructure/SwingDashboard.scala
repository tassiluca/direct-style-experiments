package io.github.tassiLuca.hub.infrastructure

import io.github.tassiLuca.hub.core.DashboardComponent
import io.github.tassiLuca.hub.infrastructure.ui.DashboardUI
import io.github.tassiLuca.hub.core.Temperature

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
