package io.github.tassiLuca.hub.adapters

import gears.async.{Async, Future, Task}
import io.github.tassiLuca.hub.core.{SensorSource, TemperatureEntry}
import io.github.tassiLuca.hub.adapters.ui.TemperatureSourceUI

class GraphicalTemperatureSource(using Async) extends SensorSource:

  override def asRunnable: Task[Unit] = Task:
    val view = TemperatureSourceUI((s, d) => publishValue(s, d))
    view.pack()
    view.setVisible(true)
    Thread.sleep(Long.MaxValue)

  private def publishValue(entry: (String, Double)): Unit = Future:
    channel.send(TemperatureEntry(sensorName = entry._1, temperature = entry._2))
