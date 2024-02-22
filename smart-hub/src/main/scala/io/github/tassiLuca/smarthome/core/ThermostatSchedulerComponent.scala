package io.github.tassiLuca.smarthome.core

import java.time.{DayOfWeek, LocalDateTime}

trait ThermostatSchedulerComponent:

  val scheduler: ThermostatScheduler

  trait ThermostatScheduler:
    type TimeSpan = (Int, Int)
    type Temperature = Double
    type Day = DayOfWeek

    def update(day: Day, timeSpan: TimeSpan, target: Temperature): Unit
    def targetFor(date: LocalDateTime): Temperature
    def currentTarget: Temperature = targetFor(LocalDateTime.now())

    extension (date: LocalDateTime) def in(t: TimeSpan): Boolean = t._1 <= date.getHour && t._2 >= date.getHour

  object ThermostatScheduler:

    def apply(targetTemperature: Double): ThermostatScheduler = ThermostatSchedulerImpl(targetTemperature)

    private class ThermostatSchedulerImpl(val targetTemperature: Double) extends ThermostatScheduler:
      private var schedule: Map[(DayOfWeek, (Int, Int)), Double] = Map()

      override def targetFor(date: LocalDateTime): Temperature = schedule
        .find((d, _) => date.getDayOfWeek == d._1 && (date in d._2))
        .map(_._2)
        .getOrElse(targetTemperature)

      override def update(day: Day, timeSpan: TimeSpan, target: Temperature): Unit =
        require(timeSpan._1 < timeSpan._2)
        schedule = schedule + ((day, timeSpan) -> target)
