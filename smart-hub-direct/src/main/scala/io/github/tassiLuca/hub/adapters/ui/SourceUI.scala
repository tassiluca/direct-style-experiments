package io.github.tassiLuca.hub.adapters.ui

import io.github.tassiLuca.dse.pimping.ScalaSwingFacade.createPanel
import io.github.tassiLuca.dse.pimping.ScalaSwingFacade.addWithRepaint
import io.github.tassiLuca.dse.pimping.ScalaSwingFacade.given

import java.awt.{BorderLayout, Component, Dimension}
import javax.swing.*

class SourceUI(sourceName: String, publishHandler: (String, Double) => Unit) extends JFrame:

  private var sensors = 0

  setTitle(s"A simulated $sourceName source in a GUI pretending to be real")
  setPreferredSize(Dimension(500, 300))
  private val addButton = JButton("Add Sensor")
  private val middle = createPanel()
  middle.setLayout(BoxLayout(middle, BoxLayout.Y_AXIS))
  addButton.addActionListener(_ => addSensorTo(middle))
  private val mainPanel = createPanel(
    (middle, BorderLayout.CENTER),
    (addButton, BorderLayout.SOUTH),
  )(using BorderLayout())
  getContentPane.add(mainPanel)

  private def addSensorTo(panel: JPanel): Component =
    sensors = sensors + 1
    val sendButton = JButton("Send")
    val sensorName = s"$sourceName-sensor-$sensors"
    val sensorInput = JTextField(4)
    sendButton.addActionListener(_ => publishHandler(sensorName, sensorInput.getText.toDouble))
    panel.addWithRepaint(createPanel(JLabel(sensorName), sensorInput, sendButton))
