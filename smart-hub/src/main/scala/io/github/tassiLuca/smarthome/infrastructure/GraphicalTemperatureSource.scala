package io.github.tassiLuca.smarthome.infrastructure

import gears.async.{Async, Task}
import io.github.tassiLuca.smarthome.core.{SensorSource, TemperatureEntry}

import java.awt.{BorderLayout, Component, Dimension}
import javax.swing.*

class GraphicalTemperatureSource extends SensorSource:

  override def asRunnable: Task[Unit] = Task:
    val view: MainView = MainView()
    view.pack()
    view.setVisible(true)
    Thread.sleep(Long.MaxValue)

  private class MainView(using Async) extends JFrame:
    import io.github.tassiLuca.utils.ScalaSwingFacade.{*, given}

    private var sensors = 0

    setTitle("A simulated temperature source in a GUI pretending to be real")
    setPreferredSize(Dimension(300, 200))
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
      val sensorPanel = createPanel(JLabel(sensorName), sensorInput, sendButton)
      sendButton.addActionListener(_ => channel.send(TemperatureEntry(sensorName, sensorInput.getText.toDouble)))
      panel.addWithRepaint(sensorPanel)
