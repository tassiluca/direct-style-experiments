package io.github.tassiLuca.smarthome.application

import gears.async.default.given
import io.github.tassiLuca.rears.filter
import io.github.tassiLuca.rears.groupBy
import gears.async.TaskSchedule.Every
import gears.async.{Async, ReadableChannel, Task}
import io.github.tassiLuca.rears.{BoundarySource, Observable}
import io.github.tassiLuca.rears.toChannel
import io.github.tassiLuca.smarthome.core.{SensorEvent, SensorSource, TemperatureEntry}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class ThermostatHubManagerTest extends AnyFlatSpec with Matchers {

  val thermostatHubManager = new ThermostatHubManager:
    override val haccController: HACController = new HACController:
      override def onHeater(): Unit = ???
      override def offHeather(): Unit = ???
      override def onAirConditioner(): Unit = ???
      override def offAirConditioner(): Unit = ???

  val sensorSource: SensorSource = new SensorSource:
    override val source: Observable[SensorEvent] = new Observable[SensorEvent]:
      private val boundarySource = BoundarySource[SensorEvent]()
      override def source: Async.Source[SensorEvent] = boundarySource
      override def asRunnable: Task[Unit] = Task {
        boundarySource.notifyListeners(TemperatureEntry(Random.nextDouble()))
      }.schedule(Every(1_000))

  "The thermostat hub manager" should "receive event from the source" in {
    Async.blocking:
      val s = sensorSource.source.source.toChannel
      val t = sensorSource.source.asRunnable.run
      s.groupBy(_.name).read() match
        case Right(("temperature", c)) => thermostatHubManager.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]])
        case _ => println("Boh")
      // t.await
  }

}
