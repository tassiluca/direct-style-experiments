package io.github.tassiLuca.analyzer.commons.client

import java.awt.BorderLayout
import javax.swing.{
  JFrame,
  WindowConstants,
  JTextField,
  JButton,
  JLabel,
  JScrollPane,
  JTable,
  JOptionPane,
  SwingUtilities,
}
import javax.swing.table.DefaultTableModel

class AnalyzerGUI(controller: AppController) extends AnalyzerView:
  private val gui: MainFrame = MainFrame(controller)

  override def run(): Unit =
    gui.pack()
    gui.setVisible(true)

  override def update(result: OrganizationReport): Unit =
    SwingUtilities.invokeLater(() => gui.updateResults(result))

  override def error(errorMessage: String): Unit =
    SwingUtilities.invokeLater(() => gui.showError(errorMessage))

  override def endComputation(): Unit =
    SwingUtilities.invokeLater(() => gui.endSession())

  private class MainFrame(controller: AppController) extends JFrame:
    import io.github.tassiLuca.utils.ScalaSwingFacade.{*, given}

    setTitle("GitHub Organization Analyzer")
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    private val inputField = JTextField(20)
    private val runButton = JButton("GO!")
    private val cancelButton = JButton("Cancel")
    private val contributionsCols = Array[AnyRef]("Login", "Contributions")
    private val contributionsModel = DefaultTableModel(contributionsCols, 0)
    private val contributionsTable = JTable(contributionsModel)
    private val repoDetailsCols = Array[AnyRef]("Name", "Issues", "Stars", "Last release")
    private val repoDetailsModel = DefaultTableModel(repoDetailsCols, 0)
    private val repoDetailsTable = JTable(repoDetailsModel)
    private val stateText = JLabel()
    private val mainPanel = createPanel(
      (createPanel(JLabel("Organization"), inputField, runButton, cancelButton), BorderLayout.NORTH),
      (createPanel(JScrollPane(contributionsTable), JScrollPane(repoDetailsTable)), BorderLayout.CENTER),
      (createPanel(stateText), BorderLayout.SOUTH),
    )(using BorderLayout())
    runButton.addActionListener(_ =>
      contributionsModel.setDataVector(Array(Array[Any]()), contributionsCols)
      repoDetailsModel.setDataVector(Array(Array[Any]()), contributionsCols)
      controller.runSession(inputField.getText)
      stateText.setText(s"Analysis of ${inputField.getText} organization ongoing..."),
    )
    cancelButton.addActionListener(_ =>
      controller.stopSession()
      stateText.setText("Computation canceled."),
    )
    getContentPane.add(mainPanel)

    def updateResults(report: OrganizationReport): Unit =
      contributionsModel.setDataVector(
        report._1.toSeq.sortBy(_._2)(using Ordering.Long.reverse).map(e => Array[Any](e._1, e._2)).toArray,
        contributionsCols,
      )
      repoDetailsModel.setDataVector(
        report._2.map(e => Array[Any](e.name, e.issues, e.stars, e.lastRelease)).toArray,
        repoDetailsCols,
      )

    def endSession(): Unit = stateText.setText("Computation ended.")

    def showError(errorMsg: String): Unit =
      endSession()
      JOptionPane.showMessageDialog(this, errorMsg, "Error!", JOptionPane.ERROR_MESSAGE)
