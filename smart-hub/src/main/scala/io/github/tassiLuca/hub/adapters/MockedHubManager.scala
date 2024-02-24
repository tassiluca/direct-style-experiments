package io.github.tassiLuca.hub.adapters

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, AsyncOperations, ReadableChannel, Task}
import io.github.tassiLuca.hub.core.{LuminosityEntry, TemperatureEntry}
import io.github.tassiLuca.hub.adapters.ui.DashboardUI
import io.github.tassiLuca.rears.groupBy

class MockedHubManager(using Async, AsyncOperations):

  private val temperatureSource = GraphicalTemperatureSource()
  private val ui = DashboardUI()
  private val thermostatHub = new MockedThermostatHubManager() with SwingDashboard(ui)
  private val lightingHub = new MockedLightingHubManager() with SwingDashboard(ui)

  def run(): Unit =
    val channelBySensor = temperatureSource.publishingChannel.groupBy(e => e.getClass)
    Task {
      channelBySensor.read() match
        case Right((clazz, c)) if clazz == classOf[TemperatureEntry] =>
          thermostatHub.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]])
        case Right((clazz, c)) if clazz == classOf[LuminosityEntry] =>
          lightingHub.run(c.asInstanceOf[ReadableChannel[LuminosityEntry]])
        case _ => ()
    }.schedule(RepeatUntilFailure()).run
    temperatureSource.asRunnable.run.await