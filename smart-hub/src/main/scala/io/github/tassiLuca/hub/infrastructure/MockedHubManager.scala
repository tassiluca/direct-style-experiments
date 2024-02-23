package io.github.tassiLuca.hub.infrastructure

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, AsyncOperations, ReadableChannel, Task}
import io.github.tassiLuca.hub.core.TemperatureEntry
import io.github.tassiLuca.rears.groupBy

class MockedHubManager(using Async, AsyncOperations):

  private val temperatureSource = GraphicalTemperatureSource()
  private val thermostatHub = new MockedThermostatHubManager() with SwingDashboard()

  def run(): Unit =
    val channelBySensor = temperatureSource.publishingChannel.groupBy(e => e.getClass)
    Task {
      channelBySensor.read() match
        case Right((clazz, c)) if clazz == classOf[TemperatureEntry] =>
          thermostatHub.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]])
        case _ => ()
    }.schedule(RepeatUntilFailure()).run
    temperatureSource.asRunnable.run.await
