package io.github.tassiLuca.hub.core

import gears.async.Async
import io.github.tassiLuca.hub.core.ports.{DashboardServiceComponent, LampsComponent}
import io.github.tassiLuca.rears.{Consumer, State}

import scala.util.Try

trait LightingSystemComponent:
  context: LampsComponent & DashboardServiceComponent =>

  val lightingSystem: LightingSystem

  trait LightingSystem
      extends Consumer[Seq[LuminosityEntry], Seq[LuminosityEntry]]
      with State[Seq[LuminosityEntry], Seq[LuminosityEntry]]

  object LightingSystem:
    def apply(): LightingSystem = LightingSystemImpl()

    private class LightingSystemImpl extends LightingSystem:
      override protected def react(e: Try[Seq[LuminosityEntry]])(using Async): Seq[LuminosityEntry] =
        println(s"[LIGHTING SYSTEM] Received $e")
        e.getOrElse(Seq())
