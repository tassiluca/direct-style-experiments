package io.github.tassiLuca.smarthome.core

import gears.async.Async

trait AlertSystemComponent:

  val alertSystem: AlertSystem

  trait AlertSystem:
    def notify(message: String)(using Async): Unit
