package io.github.tassiLuca.hub.application

import scala.concurrent.duration.DurationInt
import gears.async.{Async, AsyncOperations, ReadableChannel}
import io.github.tassiLuca.hub.core.{LightingSystemComponent, LuminosityEntry}
import io.github.tassiLuca.hub.core.ports.{DashboardServiceComponent, LampsComponent}
import io.github.tassiLuca.rears.Controller
import io.github.tassiLuca.rears.bufferWithin

trait LightingHubManager extends LightingSystemComponent with LampsComponent with DashboardServiceComponent:
  override val lightingSystem: LightingSystem = LightingSystem()

  def run(source: ReadableChannel[LuminosityEntry])(using Async, AsyncOperations): Unit =
    lightingSystem.asRunnable.run
    Controller.oneToOne(
      publisherChannel = source,
      consumer = lightingSystem,
      transformation = r => r.bufferWithin(10.seconds),
    ).run
