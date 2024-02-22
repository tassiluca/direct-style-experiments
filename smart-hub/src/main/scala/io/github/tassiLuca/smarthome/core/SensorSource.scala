package io.github.tassiLuca.smarthome.core

import io.github.tassiLuca.rears.Publisher

type Temperature = Double

/** A generic source of [[SensorEvent]]. */
trait SensorSource extends Publisher[SensorEvent]

/** A detection performed by a sensing unit. */
sealed trait SensorEvent(val name: String)
case class TemperatureEntry(sensorName: String, temperature: Temperature) extends SensorEvent(sensorName)
case class LuminosityEntry(sensorName: String, luminosity: Temperature) extends SensorEvent(sensorName)
