package io.github.tassiLuca.smarthome.application

import gears.async.TaskSchedule.{Every, RepeatUntilFailure}
import gears.async.default.given
import gears.async.{Async, Future, ReadableChannel, Task}
import io.github.tassiLuca.rears.{BoundarySource, groupBy}
import io.github.tassiLuca.smarthome.core.{SensorEvent, SensorSource, TemperatureEntry}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class ThermostatHubManagerTest extends AnyFlatSpec with Matchers {

  val thermostatHubManager: ThermostatHubManager = new ThermostatHubManager:
    override val hvacController: HVACController = new HVACController:
      override def onHeater(): Unit = ???
      override def offHeather(): Unit = ???
      override def onAirConditioner(): Unit = ???
      override def offAirConditioner(): Unit = ???

  val sensorSource: SensorSource = new SensorSource:
    private val boundarySource = BoundarySource[SensorEvent]()
    override def source: Async.Source[SensorEvent] = boundarySource
    override def asRunnable: Task[Unit] = Task {
      boundarySource.notifyListeners(TemperatureEntry(Random.nextDouble()))
    }.schedule(Every(1_000))

  "The thermostat hub manager" should "receive event from the source" in {
    Async.blocking:
      Task {
        sensorSource.publishingChannel.groupBy(_.name).read() match
          case Right(("temperature", c)) => thermostatHubManager.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]])
          case _ => println("Boh")
      }.schedule(RepeatUntilFailure()).run
      // sensorSource.asRunnable.run.await
  }
}
