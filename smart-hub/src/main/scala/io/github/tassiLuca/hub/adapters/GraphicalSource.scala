package io.github.tassiLuca.hub.adapters

import gears.async.{Async, Future, Task}
import io.github.tassiLuca.utils.ScalaSwingFacade.display
import io.github.tassiLuca.hub.adapters.ui.SourceUI
import io.github.tassiLuca.hub.core.{LuminosityEntry, SensorSource, TemperatureEntry}

class GraphicalSource(using Async) extends SensorSource:

  private val sources = Set(
    SourceUI("temperature", publishTemperatureEntry),
    SourceUI("luminosity", publishLuminosityEntry),
  )

  override def asRunnable: Task[Unit] = Task:
    sources.foreach(_.display())
    Thread.sleep(Long.MaxValue)

  private def publishTemperatureEntry(sensorName: String, temperature: Double): Unit = Future:
    channel.send(TemperatureEntry(sensorName, temperature))

  private def publishLuminosityEntry(sensorName: String, luminosity: Double): Unit = Future:
    channel.send(LuminosityEntry(sensorName, luminosity))
