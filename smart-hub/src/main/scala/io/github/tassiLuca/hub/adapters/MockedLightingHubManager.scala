package io.github.tassiLuca.hub.adapters

import gears.async.Async
import io.github.tassiLuca.hub.application.LightingHubManager

trait MockedLightingHubManager extends LightingHubManager:
  override val lamps: Lamps = new Lamps:
    override def on()(using Async): Unit = println("[Lamp] Turning On...")
    override def off()(using Async): Unit = println("[Lamp] Turning off...")
