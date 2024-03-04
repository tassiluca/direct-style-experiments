package io.github.tassiLuca.hub.adapters

import io.github.tassiLuca.hub.adapters.ui.DashboardUI
import io.github.tassiLuca.hub.core.Luminosity
import io.github.tassiLuca.hub.core.Temperature
import io.github.tassiLuca.hub.core.ports.DashboardService
import javax.swing.SwingUtilities

/** Swing dashboard. */
class SwingDashboardService : DashboardService {
    private val view = DashboardUI()

    override fun temperatureUpdated(newTemperature: Temperature) = SwingUtilities.invokeLater {
        view.temperatureLabel().text = "$newTemperature °C"
    }

    override fun luminosityUpdated(newLuminosity: Luminosity) {
        view.luminosityLabel().text = "$newLuminosity lux"
    }

    override fun onHeaterNotified() = SwingUtilities.invokeLater {
        view.heaterLabel().text = "On"
    }

    override fun offHeaterNotified() = SwingUtilities.invokeLater {
        view.heaterLabel().text = "Off"
    }

    override fun alertNotified(message: String) = SwingUtilities.invokeLater {
        view.alertsModel().insertRow(0, arrayOf(message))
    }

    override fun updateSchedule(schedule: Map<Pair<String, String>, String>) {
        view.scheduleModel().rowCount = 0
        schedule.forEach { (t, u) ->
            view.scheduleModel().addRow(arrayOf(t.first, t.second, u))
        }
    }
}
