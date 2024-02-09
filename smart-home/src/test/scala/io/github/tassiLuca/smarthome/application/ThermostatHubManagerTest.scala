package io.github.tassiLuca.smarthome.application

import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future, ReadableChannel, Task}
import io.github.tassiLuca.rears.{BoundarySource, groupBy}
import io.github.tassiLuca.smarthome.core.{SensorEvent, SensorSource, TemperatureEntry}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class ThermostatHubManagerTest extends AnyFlatSpec with Matchers {

  val thermostatHubManager: ThermostatHubManager = new ThermostatHubManager:
    override val hvacController: HVACController = new HVACController:
      override def onHeater(using Async): Unit = ???
      override def offHeather(using Async): Unit = ???
      override def onAirConditioner(using Async): Unit = ???
      override def offAirConditioner(using Async): Unit = ???

    override val alertSystem: AlertSystem = new AlertSystem:
      override def notify(message: String)(using Async): Unit = ???

  val sensorSource: SensorSource = new SensorSource:
    var i = 0
    private val boundarySource = BoundarySource[SensorEvent]()
    override def source: Async.Source[SensorEvent] = boundarySource
    override def asRunnable: Task[Unit] = Task {
      boundarySource.notifyListeners(TemperatureEntry(i))
      i = i + 1
    }.schedule(Every(1_000))

  "The thermostat hub manager" should "receive event from the source" in {
    Async.blocking:
      val channelBySensor = sensorSource.publishingChannel.groupBy(_.name)
      Task {
        channelBySensor.read() match
          case Right(("temperature", c)) => thermostatHubManager.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]])
          case _ => ()
      }.schedule(RepeatUntilFailure()).run
      // sensorSource.asRunnable.run.await
  }
}
