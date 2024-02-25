package io.github.tassiLuca.hubkt.adapters

import io.github.tassiLuca.hubkt.adapters.ui.DashboardUI
import io.github.tassiLuca.hubkt.core.Temperature
import io.github.tassiLuca.hubkt.core.ports.DashboardService

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
