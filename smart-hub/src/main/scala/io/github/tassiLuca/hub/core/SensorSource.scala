package io.github.tassiLuca.hub.core

import io.github.tassiLuca.rears.Publisher

/** The temperature value, in a certain moment in time, expressed in °C. */
type Temperature = Double

/** The value of luminosity, in a certain moment in time. */
type Luminosity = Double

/** A generic source of [[SensorEvent]] (e.g. a MQTT broker). */
trait SensorSource extends Publisher[SensorEvent]

/** A detection performed by a sensing unit. */
sealed trait SensorEvent(val name: String)

/** A temperature detection. */
case class TemperatureEntry(sensorName: String, temperature: Temperature) extends SensorEvent(sensorName)

/** A luminosity detection. */
case class LuminosityEntry(sensorName: String, luminosity: Temperature) extends SensorEvent(sensorName)
