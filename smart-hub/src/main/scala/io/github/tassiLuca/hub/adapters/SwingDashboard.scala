package io.github.tassiLuca.hub.adapters

import io.github.tassiLuca.hub.adapters.ui.DashboardUI
import io.github.tassiLuca.hub.core.Temperature
import io.github.tassiLuca.hub.core.ports.DashboardServiceComponent

import javax.swing.SwingUtilities

trait SwingDashboard(view: DashboardUI) extends DashboardServiceComponent:
  override val dashboard: DashboardService = new DashboardService:
    
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
      view.alertsModel.insertRow(0, Array[AnyRef](msg))
    }
