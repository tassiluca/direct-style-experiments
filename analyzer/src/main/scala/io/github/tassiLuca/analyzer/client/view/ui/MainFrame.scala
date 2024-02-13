package io.github.tassiLuca.analyzer.client.view.ui

import io.github.tassiLuca.analyzer.client.{AppController, OrganizationReport}

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{BorderLayout, FlowLayout, GridLayout}
import javax.swing.table.DefaultTableModel
import javax.swing.JFrame
import javax.swing.*

class MainFrame(controller: AppController) extends JFrame:
  setTitle("GitHub Organization Analyzer")
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  val topPanel = new JPanel(new FlowLayout())
  val inputLabel = new JLabel("Organization:")
  val inputField = new JTextField(20)
  val runButton = new JButton("GO!")
  val cancelButton = new JButton("Cancel")
  topPanel.add(inputLabel)
  topPanel.add(inputField)
  topPanel.add(runButton)
  topPanel.add(cancelButton)
  val contributionsCols = Array[AnyRef]("Login", "Contributions")
  val contributionsModel = DefaultTableModel(contributionsCols, 0)
  val contributionsTable = new JTable(contributionsModel)
  val repoDetailsCols = Array[AnyRef]("Name", "Issues", "Stars", "Last release")
  val repoDetailsModel = DefaultTableModel(repoDetailsCols, 0)
  val repoDetailsTable = new JTable(repoDetailsModel)
  val scrollPane1 = new JScrollPane(contributionsTable)
  val scrollPane2 = new JScrollPane(repoDetailsTable)
  val middlePanel = new JPanel(new GridLayout(2, 1))
  middlePanel.add(scrollPane1)
  middlePanel.add(scrollPane2)
  val stateText = new JLabel()
  val bottomPanel = new JPanel(new FlowLayout())
  bottomPanel.add(stateText)
  val mainPanel = new JPanel(new BorderLayout())
  mainPanel.add(topPanel, BorderLayout.NORTH)
  mainPanel.add(middlePanel, BorderLayout.CENTER)
  mainPanel.add(bottomPanel, BorderLayout.SOUTH)
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
    JOptionPane.showMessageDialog(this, errorMsg, "Error!", JOptionPane.ERROR_MESSAGE)
