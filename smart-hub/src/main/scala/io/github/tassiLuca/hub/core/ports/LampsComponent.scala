package io.github.tassiLuca.hub.core.ports

import gears.async.Async

trait LampsComponent:

  val lamps: Lamps

  trait Lamps:
    def on()(using Async): Unit
    def off()(using Async): Unit
