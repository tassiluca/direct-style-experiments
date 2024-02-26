package io.github.tassiLuca.hub.core

import java.time.{DayOfWeek, LocalDateTime}

/** A thermostat scheduler managing target temperature based on schedules for different days and times. */
trait ThermostatScheduler:
  /** A span of time, the minimum unit of temperature customization. */
  type TimeSpan

  /** A day of the week. */
  type Day

  /** Updates the [[target]] temperature for the given [[day]] and [[timeSpan]]. */
  def update(day: Day, timeSpan: TimeSpan, target: Temperature): Unit

  /** @return the target temperature for the given [[date]]. */
  def targetFor(date: LocalDateTime): Temperature

  /** @return the current target temperature. */
  def currentTarget: Temperature = targetFor(LocalDateTime.now())

  extension (date: LocalDateTime)
    /** @return true if [[date]] is in between the given [[timespan]], false otherwise. */
    def in(timespan: TimeSpan): Boolean

/** A [[ThermostatScheduler]] managing target temperature based on hourly schedules per week day. */
trait ThermostatHourlyScheduler extends ThermostatScheduler:
  override type TimeSpan = (Int, Int)
  override type Day = DayOfWeek
  type Schedule = Map[(Day, TimeSpan), Temperature]

object ThermostatScheduler:

  /** Creates a [[ThermostatHourlyScheduler]], initially set up with the given [[target]] temperature. */
  def byHour(target: Temperature): ThermostatHourlyScheduler = ThermostatSchedulerImpl(target)

  private class ThermostatSchedulerImpl(val target: Temperature) extends ThermostatHourlyScheduler:

    private var schedule: Schedule = Map()

    override def targetFor(date: LocalDateTime): Temperature =
      schedule.find((d, _) => date.getDayOfWeek == d._1 && (date in d._2)).map(_._2).getOrElse(target)

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
