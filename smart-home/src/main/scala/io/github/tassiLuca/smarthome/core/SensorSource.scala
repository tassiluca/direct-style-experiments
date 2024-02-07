package io.github.tassiLuca.smarthome.core

import io.github.tassiLuca.rears.Publisher

/** A generic source of [[SensorEvent]]. */
trait SensorSource extends Publisher[SensorEvent]

/** A detection performed by a sensing unit. */
sealed trait SensorEvent(val name: String)
case class TemperatureEntry(temperature: Double) extends SensorEvent("temperature")
case class LuminosityEntry(luminosity: Double) extends SensorEvent("luminosity")
