package io.github.tassiLuca.analyzer.client.view

import io.github.tassiLuca.analyzer.client.view.ui.MainFrame
import io.github.tassiLuca.analyzer.client.{AnalyzerView, AppController, OrganizationReport}

import javax.swing.SwingUtilities

class AnalyzerGUI(controller: AppController) extends AnalyzerView:
  val gui = MainFrame(controller)

  override def run(): Unit =
    gui.pack()
    gui.setVisible(true)

  override def update(result: OrganizationReport): Unit =
    println(result)
    SwingUtilities.invokeLater(() => gui.updateResults(result))
    println("-".repeat(100))
