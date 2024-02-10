package io.github.tassiLuca.smarthome.core

import io.github.tassiLuca.rears.Publisher

/** A generic source of [[SensorEvent]]. */
trait SensorSource extends Publisher[SensorEvent]

/** A detection performed by a sensing unit. */
sealed trait SensorEvent(val name: String)
case class TemperatureEntry(sensorName: String, temperature: Double) extends SensorEvent(sensorName)
case class LuminosityEntry(sensorName: String, luminosity: Double) extends SensorEvent(sensorName)
