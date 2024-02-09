package io.github.tassiLuca.smarthome.core

import io.github.tassiLuca.rears.Consumer

import scala.util.Try

trait SensorHealthCheckerComponent[E <: SensorEvent]:
  alertSystem: AlertSystemComponent =>

  val sensorHealthChecker: SensorHealthChecker

  trait SensorHealthChecker extends Consumer[E]

  object SensorHealthChecker:

    def apply(): SensorHealthChecker = SensorHealthCheckerImpl()

    private class SensorHealthCheckerImpl extends SensorHealthChecker:
      override protected def react(e: Try[E]): Unit =
        println(s"[SENSOR HEALTH CHECKER] Received $e")
