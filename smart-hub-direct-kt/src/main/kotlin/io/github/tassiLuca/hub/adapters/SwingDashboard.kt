package io.github.tassiLuca.hub.adapters

import io.github.tassiLuca.hub.adapters.ui.DashboardUI
import io.github.tassiLuca.hub.core.Temperature
import io.github.tassiLuca.hub.core.ports.DashboardService

/** Swing dashboard. */
class SwingDashboard : DashboardService {
    private val view = DashboardUI()

    override fun temperatureUpdated(newTemperature: Temperature) {
        view.temperatureLabel.setText("$newTemperature °C")
    }

    override fun onHeaterNotified() {
        view.heaterLabel.setText("Off")
    }

    override fun offHeaterNotified() {
        view.heaterLabel.setText("On")
    }

    override fun alertNotified(msg: String) {
        view.alertsModel.insertRow(0, arrayOf(msg))
    }
}
