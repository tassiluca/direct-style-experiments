package io.github.tassiLuca.smarthome.core

import gears.async.Async
import io.github.tassiLuca.rears.{Consumer, State}

import scala.util.{Failure, Success, Try}

trait SensorHealthCheckerComponent[E <: SensorEvent]:
  context: AlertSystemComponent with DashboardComponent =>

  /** The [[SensorHealthChecker]] instance. */
  val sensorHealthChecker: SensorHealthChecker

  /** A generic consumer of [[SensorEvent]] that detects */
  trait SensorHealthChecker extends Consumer[Seq[E]] with State[Seq[E]]

  object SensorHealthChecker:

    def apply(): SensorHealthChecker = SensorHealthCheckerImpl()

    private class SensorHealthCheckerImpl extends SensorHealthChecker:

      override protected def react(e: Try[Seq[E]])(using Async): Unit = e match
        case Success(es) =>
          if state.isDefined && es.map(_.name) != state.map(_.map(_.name)).get then
            context.alertSystem.notify(s"Detected a change: current state is $state, new e is $es")
            context.dashboard.newAlert(s"Detected a change: current state is $state, new e is $es")
        case Failure(es) => context.alertSystem.notify(es.getMessage)
