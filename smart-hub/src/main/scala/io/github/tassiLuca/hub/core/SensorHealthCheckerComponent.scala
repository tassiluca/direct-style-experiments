package io.github.tassiLuca.hub.core

import gears.async.Async
import io.github.tassiLuca.hub.core.ports.{AlertSystemComponent, DashboardServiceComponent}
import io.github.tassiLuca.rears.{Consumer, State}

import java.util.Date
import scala.util.{Failure, Success, Try}

trait SensorHealthCheckerComponent[E <: SensorEvent]:
  context: AlertSystemComponent & DashboardServiceComponent =>

  /** The [[SensorHealthChecker]] instance. */
  val sensorHealthChecker: SensorHealthChecker

  /** A generic consumer of [[SensorEvent]] that detects probable sensing unit malfunctioning. */
  trait SensorHealthChecker extends Consumer[Seq[E], Seq[E]] with State[Seq[E], Seq[E]]

  object SensorHealthChecker:

    def apply(): SensorHealthChecker = SensorHealthCheckerImpl()

    private class SensorHealthCheckerImpl extends SensorHealthChecker:

      override protected def react(e: Try[Seq[E]])(using Async): Seq[E] = e match
        case Success(current) =>
          val noMoreActiveSensors = state.map(ex => ex.map(_.name).toSet -- current.map(_.name).toSet)
          if noMoreActiveSensors.isDefined && noMoreActiveSensors.get.nonEmpty then
            val alertMessage = s"[${Date()}] Detected ${noMoreActiveSensors.get.mkString(", ")} no more active!"
            context.alertSystem.notify(alertMessage)
            context.dashboard.alertNotified(alertMessage)
          current
        case Failure(es) => context.alertSystem.notify(es.getMessage); Seq()
