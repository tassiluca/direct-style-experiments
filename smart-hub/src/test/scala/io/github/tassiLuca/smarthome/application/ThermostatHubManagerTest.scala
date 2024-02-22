package io.github.tassiLuca.smarthome.application

import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future, ReadableChannel, Task}
import io.github.tassiLuca.rears.groupBy
import io.github.tassiLuca.smarthome.core.{SensorEvent, SensorSource, Temperature, TemperatureEntry}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class ThermostatHubManagerTest extends AnyFlatSpec with Matchers {

  val thermostatHubManager: ThermostatHubManager = new ThermostatHubManager:
    override val heater: Heater = new Heater:
      override def on()(using Async): Unit = ???
      override def off()(using Async): Unit = ???
      override def state: HeaterState = ???

    override val dashboard: Dashboard = new Dashboard:
      override def temperatureUpdated(newTemperature: Temperature): Unit = ???
      override def alertNotified(msg: String): Unit = ???
      override def onHeaterNotified(): Unit = ???
      override def offHeaterNotified(): Unit = ???

    override val alertSystem: AlertSystem = new AlertSystem:
      override def notify(message: String)(using Async): Unit = ???

  val sensorSource: SensorSource = new SensorSource:
    override def asRunnable: Task[Unit] = Task {
      val v1 = Random.nextDouble()
      println(s"publishing $v1")
      channel.send(TemperatureEntry("t2-ada", v1))
      val v2 = Random.nextDouble()
      println(s"publishing $v2")
      channel.send(TemperatureEntry(Random.nextString(6), v2))
    }.schedule(Every(15_000))

  "The thermostat hub manager" should "receive event from the source" in {
    Async.blocking:
      val channelBySensor = sensorSource.publishingChannel.groupBy(e => e.getClass)
      Task {
        channelBySensor.read() match
          case Right((clazz, c)) if clazz == classOf[TemperatureEntry] =>
            thermostatHubManager.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]])
          case _ => ()
      }.schedule(RepeatUntilFailure()).run
      // sensorSource.asRunnable.run.await
  }
}
