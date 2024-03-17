package io.github.tassiLuca.hub.application

import gears.async.{Async, Channel, SendableChannel, Task, UnboundedChannel}
import io.github.tassiLuca.hub.core.TemperatureEntry

def sensorSource[T](
    publishLogic: Async ?=> SendableChannel[T] => Unit,
)(using Async): (Channel[T], Task[Unit]) =
  val channel = UnboundedChannel[T]()
  (channel, Task { publishLogic(channel.asSendable) })
