package io.github.tassiLuca.analyzerkt.client

import io.github.tassiLuca.analyzer.commons.client.AppController
import io.github.tassiLuca.analyzer.commons.client.ui.MainFrame
import javax.swing.SwingUtilities

/** The analyzer application GUI. */
class AnalyzerGUI(controller: AppController) : AnalyzerView {
    private val gui: MainFrame = MainFrame(controller)

    override fun run() {
        gui.pack()
        gui.isVisible = true
    }

    override fun update(result: OrganizationReport) = SwingUtilities.invokeLater {
        gui.contributionsModel().setDataVector(
            result.first.toList().sortedByDescending { it.second }.map { arrayOf(it.first, it.second) }.toTypedArray(),
            gui.contributionsCols(),
        )
        gui.repoDetailsModel().setDataVector(
            result.second.map { arrayOf(it.name, it.issues, it.stars, it.lastRelease) }.toTypedArray(),
            gui.repoDetailsCols(),
        )
    }

    override fun endComputation() = SwingUtilities.invokeLater { gui.ended() }

    override fun error(errorMessage: String) = SwingUtilities.invokeLater { gui.showError(errorMessage) }

    override fun cancelled() = SwingUtilities.invokeLater { gui.cancelled() }
}
