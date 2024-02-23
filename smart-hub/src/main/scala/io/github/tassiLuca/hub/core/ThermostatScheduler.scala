package io.github.tassiLuca.hub.core

import java.time.{DayOfWeek, LocalDateTime}

trait ThermostatScheduler:
  type TimeSpan
  type Day

  def update(day: Day, timeSpan: TimeSpan, target: Temperature): Unit
  def targetFor(date: LocalDateTime): Temperature
  def currentTarget: Temperature = targetFor(LocalDateTime.now())

  extension (date: LocalDateTime) def in(t: TimeSpan): Boolean

trait ThermostatHourlyScheduler extends ThermostatScheduler:
  override type TimeSpan = (Int, Int)
  override type Day = DayOfWeek
  type Schedule = Map[(Day, TimeSpan), Temperature]

object ThermostatScheduler:

  def byHour(targetTemperature: Temperature): ThermostatHourlyScheduler = ThermostatSchedulerImpl(targetTemperature)

  private class ThermostatSchedulerImpl(val targetTemperature: Temperature) extends ThermostatHourlyScheduler:

    private var schedule: Schedule = Map()

    override def targetFor(date: LocalDateTime): Temperature = schedule
      .find((d, _) => date.getDayOfWeek == d._1 && (date in d._2))
      .map(_._2)
      .getOrElse(targetTemperature)

    override def update(day: Day, timeSpan: TimeSpan, target: Temperature): Unit =
      require(timeSpan._1 < timeSpan._2, "The timespan end must be greater than the start")
      val newEntry = (day, timeSpan) -> target
      if schedule.isEmpty then schedule = schedule + newEntry
      else
        val (nonOverlapping, overlapping) = overlappingIn(day, timeSpan)
        val splitEntries = overlapping.splitFor(timeSpan)
        schedule = (nonOverlapping ++ splitEntries) + newEntry

    extension (date: LocalDateTime)
      override infix def in(timeSpan: TimeSpan): Boolean =
        timeSpan._1 <= date.getHour && timeSpan._2 >= date.getHour

    private def overlappingIn(day: Day, timeSpan: TimeSpan) =
      schedule.partition { case ((d, (start, end)), _) => d == day && (end <= timeSpan._1 || start >= timeSpan._2) }

    extension (schedule: Schedule)
      private def splitFor(timeSpan: TimeSpan) =
        schedule.flatMap { case ((day, (start, end)), currentTarget) =>
          (day, (start, timeSpan._1)) -> currentTarget :: (day, (timeSpan._2, end)) -> currentTarget :: Nil
        }.filter { case ((_, (start, end)), _) => start < end }
