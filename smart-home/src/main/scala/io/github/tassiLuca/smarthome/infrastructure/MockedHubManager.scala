package io.github.tassiLuca.smarthome.infrastructure

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, AsyncOperations, ReadableChannel, Task}
import io.github.tassiLuca.rears.groupBy
import io.github.tassiLuca.smarthome.core.TemperatureEntry

object MockedHubManager:

  private val temperatureSource = GraphicalTemperatureSource()
  private val thermostatHub = MockedThermostatHubManager()

  def run(using Async, AsyncOperations): Unit =
    val channelBySensor = temperatureSource.publishingChannel.groupBy(e => e.getClass)
    Task {
      channelBySensor.read() match
        case Right((clazz, c)) if clazz == classOf[TemperatureEntry] =>
          thermostatHub.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]])
        case _ => ()
    }.schedule(RepeatUntilFailure()).run
    temperatureSource.asRunnable.run.await
