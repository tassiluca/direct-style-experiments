package io.github.tassiLuca.analyzer.client.view

import io.github.tassiLuca.analyzer.client.view.ui.MainFrame
import io.github.tassiLuca.analyzer.client.{AnalyzerView, AppController, OrganizationReport}

import javax.swing.SwingUtilities

class AnalyzerGUI(controller: AppController) extends AnalyzerView:
  private val gui: MainFrame = MainFrame(controller)

  override def run(): Unit =
    gui.pack()
    gui.setVisible(true)

  override def update(result: OrganizationReport): Unit =
    SwingUtilities.invokeLater(() => gui.updateResults(result))

  override def error(errorMessage: String): Unit = gui.showError(errorMessage)

  override def endComputation(): Unit = gui.endSession()
