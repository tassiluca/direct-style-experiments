package io.github.tassiLuca.analyzer.commons.client

import io.github.tassiLuca.analyzer.commons.client.ui.MainFrame
import io.github.tassiLuca.pimping.ScalaSwingFacade.display

import javax.swing.SwingUtilities

class AnalyzerGUI(controller: AppController) extends AnalyzerView:
  private val gui: MainFrame = MainFrame(controller)

  override def run(): Unit =
    gui.display()
    Thread.sleep(Long.MaxValue)

  override def update(result: OrganizationReport): Unit = SwingUtilities.invokeLater(() =>
    gui.contributionsModel.setDataVector(
      result._1.toSeq.sortBy(_._2)(using Ordering.Long.reverse).map(e => Array[Any](e._1, e._2)).toArray,
      gui.contributionsCols,
    )
    gui.repoDetailsModel.setDataVector(
      result._2.map(e => Array[Any](e.name, e.issues, e.stars, e.lastRelease)).toArray,
      gui.repoDetailsCols,
    ),
  )

  override def error(errorMessage: String): Unit =
    SwingUtilities.invokeLater(() => gui.showError(errorMessage))

  override def endComputation(): Unit =
    SwingUtilities.invokeLater(() => gui.ended())

  override def cancelled(): Unit = SwingUtilities.invokeLater(() => gui.cancelled())
