package io.github.tassiLuca.analyzer.commons.client.ui

import io.github.tassiLuca.analyzer.commons.client.AppController

import java.awt.BorderLayout
import javax.swing.*
import javax.swing.table.DefaultTableModel

class MainFrame(controller: AppController) extends JFrame:
  import io.github.tassiLuca.utils.ScalaSwingFacade.{*, given}

  setTitle("GitHub Organization Analyzer")
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  private val inputField = JTextField(20)
  private val runButton = JButton("GO!")
  private val cancelButton = JButton("Cancel")
  val contributionsCols: Array[AnyRef] = Array[AnyRef]("Login", "Contributions")
  val contributionsModel: DefaultTableModel = DefaultTableModel(contributionsCols, 0)
  private val contributionsTable = JTable(contributionsModel)
  val repoDetailsCols: Array[AnyRef] = Array[AnyRef]("Name", "Issues", "Stars", "Last release")
  val repoDetailsModel: DefaultTableModel = DefaultTableModel(repoDetailsCols, 0)
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

  def showError(errorMsg: String): Unit =
    endSession()
    JOptionPane.showMessageDialog(this, errorMsg, "Error!", JOptionPane.ERROR_MESSAGE)

  def endSession(): Unit = stateText.setText("Computation ended.")

  def cancelled(): Unit = stateText.setText("Computation canceled.")