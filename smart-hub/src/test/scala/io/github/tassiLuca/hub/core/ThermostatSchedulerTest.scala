package io.github.tassiLuca.hub.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{DayOfWeek, LocalDate, LocalDateTime, LocalTime}

class ThermostatSchedulerTest extends AnyFlatSpec with Matchers {

  private val targetTemperature = 19.5
  private val scheduler = ThermostatScheduler.byHour(targetTemperature)
  private val aMondayDay = LocalDate.of(2024, 2, 5)
  private val midMinutes = 30

  "ThermostatScheduler" should s"be initialized with a target temperature of $targetTemperature" in {
    scheduler.currentTarget shouldBe targetTemperature
  }

  "ThermostatScheduler when updated" should "update correctly the schedule" in {
    val target = 21.0
    val timeSpan = 9 -> 10
    val samplingTime = LocalTime.of(timeSpan._1, midMinutes)
    scheduler.update(DayOfWeek.MONDAY, timeSpan, target)
    scheduler.targetFor(LocalDateTime.of(aMondayDay, samplingTime)) shouldBe target
  }

  "ThermostatScheduler when updated with overlapping timespan" should "update correctly the schedule" in {
    val target = 19.0
    val (start, end) = (9, 13)
    val schedule = Map(
      (8 -> 9) -> 19.0,
      (9 -> 10) -> 21.0,
      (10 -> 12) -> 20.0,
      (12 -> 15) -> 22.0,
    )
    schedule.foreach { e => scheduler.update(DayOfWeek.MONDAY, e._1, e._2) }
    scheduler.update(DayOfWeek.MONDAY, start -> end, target)
    for i <- start until end do
      scheduler.targetFor(LocalDateTime.of(aMondayDay, LocalTime.of(i, midMinutes))) shouldBe target
    schedule.foreach { e =>
      if e._1._1 < start then
        scheduler.targetFor(LocalDateTime.of(aMondayDay, LocalTime.of(e._1._1, midMinutes))) shouldBe e._2
    }
  }
}
