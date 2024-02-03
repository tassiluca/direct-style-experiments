package io.github.tassiLuca.smarthome.core

import gears.async.{SendableChannel, UnboundedChannel}
import io.github.tassiLuca.rears.Consumer

trait Thermostat extends Consumer[SensorsEvent.TemperatureEntry]:
  override def listeningChannel: SendableChannel[SensorsEvent.TemperatureEntry] =
    UnboundedChannel()

  override def react(e: SensorsEvent.TemperatureEntry): Unit = ???
