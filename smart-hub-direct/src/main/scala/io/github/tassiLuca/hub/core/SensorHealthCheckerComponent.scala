package io.github.tassiLuca.hub.core

import gears.async.{Async, Future}
import io.github.tassiLuca.hub.core.ports.{AlertSystemComponent, DashboardServiceComponent}
import io.github.tassiLuca.rears.{Consumer, State}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

/** The component encapsulating the [[SensorHealthChecker]] entity. */
trait SensorHealthCheckerComponent[E <: SensorEvent]:
  context: AlertSystemComponent & DashboardServiceComponent =>

  /** The [[SensorHealthChecker]] instance. */
  val sensorHealthChecker: SensorHealthChecker

  /** A [[state]]ful consumer of [[SensorEvent]] detecting possible malfunctioning and
    * keeping track of last known active sensing units.
    */
  trait SensorHealthChecker extends Consumer[Seq[E], Seq[E]] with State[Seq[E], Seq[E]]

  object SensorHealthChecker:
    def apply(): SensorHealthChecker = SensorHealthCheckerImpl()

    private class SensorHealthCheckerImpl extends SensorHealthChecker with State[Seq[E], Seq[E]](Seq()):

      override protected def react(e: Try[Seq[E]])(using Async.Spawn): Seq[E] = e match
        case Success(current) =>
          val noMoreActive = state.map(_.name).toSet -- current.map(_.name).toSet
          if noMoreActive.nonEmpty then sendAlert(s"[$currentTime] ${noMoreActive.mkString(", ")} no more active!")
          current
        case Failure(es) => sendAlert(es.getMessage); Seq()

      private def sendAlert(message: String)(using Async.Spawn): Unit = Future:
        context.alertSystem.notify(message)
        context.dashboard.alertNotified(message)

      private def currentTime: String =
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        currentTime.format(formatter)
