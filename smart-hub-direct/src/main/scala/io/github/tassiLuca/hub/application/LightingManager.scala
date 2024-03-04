package io.github.tassiLuca.hub.application

import gears.async.{Async, AsyncOperations, ReadableChannel}
import io.github.tassiLuca.hub.core.ports.{DashboardServiceComponent, LampsComponent}
import io.github.tassiLuca.hub.core.{LightingSystemComponent, LuminosityEntry}
import io.github.tassiLuca.rears.{Controller, bufferWithin}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/** The hub managing the lighting system. */
trait LightingManager extends LightingSystemComponent with LampsComponent with DashboardServiceComponent:
  override val lightingSystem: LightingSystem = LightingSystem()
  private val samplingWindow = 10 seconds

  /** Runs the manager, spawning a new controller consuming the given [[source]] of events. */
  def run(source: ReadableChannel[LuminosityEntry])(using Async, AsyncOperations): Unit =
    lightingSystem.asRunnable.run
    Controller.oneToOne(
      publisherChannel = source,
      consumer = lightingSystem,
      transformation = r => r.bufferWithin(samplingWindow),
    ).run
