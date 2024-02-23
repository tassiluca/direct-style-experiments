package io.github.tassiLuca.hub.infrastructure

import io.github.tassiLuca.hub.infrastructure.ui.DashboardUI
import io.github.tassiLuca.hub.core.Temperature
import io.github.tassiLuca.hub.core.ports.DashboardServiceComponent

import javax.swing.SwingUtilities

trait SwingDashboard extends DashboardServiceComponent:
  override val dashboard: DashboardService = new DashboardService:

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
