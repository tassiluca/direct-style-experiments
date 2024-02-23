package io.github.tassiLuca.hub.infrastructure.ui

import java.awt.{BorderLayout, Component, Dimension}
import javax.swing.*

class TemperatureSourceUI(publishHandler: (String, Double) => Unit) extends JFrame:

  import io.github.tassiLuca.utils.ScalaSwingFacade.{*, given}

  private var sensors = 0

  setTitle("A simulated temperature source in a GUI pretending to be real")
  setPreferredSize(Dimension(500, 300))
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  private val addButton = JButton("Add Sensor")
  private val middle = createPanel()
  middle.setLayout(BoxLayout(middle, BoxLayout.Y_AXIS))
  addButton.addActionListener(_ => addSensorTo(middle))
  private val mainPanel = createPanel(
    (addButton, BorderLayout.NORTH),
    (middle, BorderLayout.CENTER),
  )(using BorderLayout())
  getContentPane.add(mainPanel)

  private def addSensorTo(panel: JPanel): Component =
    sensors = sensors + 1
    val sendButton = JButton("Send")
    val sensorName = s"sensor-$sensors"
    val sensorInput = JTextField(4)
    sendButton.addActionListener(_ => publishHandler(sensorName, sensorInput.getText.toDouble))
    panel.addWithRepaint(createPanel(JLabel(sensorName), sensorInput, sendButton))
