package io.github.tassiLuca.hub.adapters

import gears.async.Async
import io.github.tassiLuca.hub.application.LightingManager

trait MockedLightingManager extends LightingManager:
  override val lamps: LampsController = new LampsController:
    override def on()(using Async): Unit = println("[Lamp] Turning On...")
    override def off()(using Async): Unit = println("[Lamp] Turning off...")
