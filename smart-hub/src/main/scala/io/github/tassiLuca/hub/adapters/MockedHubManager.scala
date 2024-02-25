package io.github.tassiLuca.hub.adapters

import scala.concurrent.duration.DurationInt
import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, AsyncOperations, ReadableChannel, Task}
import io.github.tassiLuca.hub.core.{LuminosityEntry, TemperatureEntry}
import io.github.tassiLuca.hub.adapters.ui.DashboardUI
import io.github.tassiLuca.rears.groupBy

import scala.concurrent.duration.Duration
import scala.language.postfixOps

class MockedHubManager(using Async, AsyncOperations):

  private val samplingWindow: Duration = 5 seconds
  private val ui = DashboardUI()
  private val sensorsSource = GraphicalSource()
  private val thermostatHub = new MockedThermostatHubManager() with SwingDashboard(ui)
  private val lightingHub = new MockedLightingHubManager() with SwingDashboard(ui)

  def run(): Unit =
    val channelBySensor = sensorsSource.publishingChannel.groupBy(e => e.getClass)
    Task {
      channelBySensor.read() match
        case Right((clazz, c)) if clazz == classOf[TemperatureEntry] =>
          thermostatHub.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]], samplingWindow)
        case Right((clazz, c)) if clazz == classOf[LuminosityEntry] =>
          lightingHub.run(c.asInstanceOf[ReadableChannel[LuminosityEntry]], samplingWindow)
        case _ => ()
    }.schedule(RepeatUntilFailure()).run
    sensorsSource.asRunnable.run.await
