package io.github.tassiLuca.hub.application

import gears.async.default.given
import gears.async.{Async, AsyncOperations, Channel, ReadableChannel, SendableChannel, Task, UnboundedChannel}
import io.github.tassiLuca.hub.core.LuminosityEntry
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LightingManagerTest extends AnyFlatSpec with Matchers {

  private val thermostatManager = TestableLightingManager

  "The lighting system" should "receive event from the source" in {
    Async.blocking:
      val (channel, task) = sensorSource[LuminosityEntry]: c =>
        c.send(LuminosityEntry("l1", 500))
        c.send(LuminosityEntry("l2", 700L))
      thermostatManager.run(channel)
      task.start().awaitResult
      AsyncOperations.sleep(samplingWindow.toMillis + 1_000)
      thermostatManager.dashboardMessages should contain(Message.LuminosityUpdate)
      channel.close()
  }
}
