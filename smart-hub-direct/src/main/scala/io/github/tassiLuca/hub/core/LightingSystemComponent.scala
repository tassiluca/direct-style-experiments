package io.github.tassiLuca.hub.core

import gears.async.Async
import io.github.tassiLuca.hub.core.ports.{DashboardServiceComponent, LampsComponent}
import io.github.tassiLuca.rears.Consumer

import scala.util.Try

/** The component encapsulating the [[LightingSystem]] entity. */
trait LightingSystemComponent:
  context: LampsComponent & DashboardServiceComponent =>

  /** The [[LightingSystem]] instance. */
  val lightingSystem: LightingSystem

  /** A consumer of [[LuminosityEntry]], in charge of controlling the lamps. */
  trait LightingSystem extends Consumer[Seq[LuminosityEntry], Unit]

  object LightingSystem:
    def apply(): LightingSystem = LightingSystemImpl()

    private class LightingSystemImpl extends LightingSystem:
      override protected def react(e: Try[Seq[LuminosityEntry]])(using Async): Unit =
        val averageLuminosity = e.map { entries => entries.map(_.luminosity).sum / entries.size }.toOption
        averageLuminosity.foreach(context.dashboard.luminosityUpdate(_))
