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
  val repoDetailsModel = DefaultTableModel(Array[AnyRef]("Name", "Issues", "Stars", "Last release"), 0)
  val repoDetailsTable = new JTable(repoDetailsModel)
  val scrollPane1 = new JScrollPane(contributionsTable)
  val scrollPane2 = new JScrollPane(repoDetailsTable)
  val middlePanel = new JPanel(new GridLayout(2, 1))
  middlePanel.add(scrollPane1)
  middlePanel.add(scrollPane2)
  // Bottom Panel
  val stateText = new JLabel()
  val bottomPanel = new JPanel(new FlowLayout())
  bottomPanel.add(stateText)
  // Main Panel
  val mainPanel = new JPanel(new BorderLayout())
  mainPanel.add(topPanel, BorderLayout.NORTH)
  mainPanel.add(middlePanel, BorderLayout.CENTER)
  mainPanel.add(bottomPanel, BorderLayout.SOUTH)

  // Define behavior for buttons
  runButton.addActionListener((e: ActionEvent) =>
    stateText.setText("Computation running..."),
  )
  cancelButton.addActionListener((e: ActionEvent) =>
    // Code to cancel computation
    stateText.setText("Computation canceled."),
  )
  // Set up the main frame
  getContentPane.add(mainPanel)
  pack()

  def updateResults(report: OrganizationReport): Unit =
    contributionsModel.setDataVector(
      report._1.map(e => Array[Any](e._1, e._2)).toArray,
      contributionsCols,
    )
