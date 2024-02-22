package io.github.tassiLuca.smarthome.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{DayOfWeek, LocalDateTime}

class ThermostatSchedulerTest extends AnyFlatSpec with Matchers {

  private val targetTemperature = 19.5
  private val thermostatScheduler = ThermostatScheduler.byHour(targetTemperature)

  "ThermostatScheduler" should s"be initialized to have a target temperature of $targetTemperature" in {
    thermostatScheduler.currentTarget shouldBe targetTemperature
  }

  "ThermostatScheduler when updated" should "update correctly the schedule" in {
    thermostatScheduler.update(DayOfWeek.MONDAY, 9 -> 10, 21.0)
    thermostatScheduler.targetFor(LocalDateTime.of(2024, 2, 5, 9, 30)) shouldBe 21.0
  }

  "ThermostatScheduler when updated with overlapping timespan" should "update correctly the schedule" in {
    val (start, end) = (9, 13)
    val target = 19.0
    thermostatScheduler.update(DayOfWeek.MONDAY, 8 -> 9, 19.0)
    thermostatScheduler.update(DayOfWeek.MONDAY, 9 -> 10, 21.0)
    thermostatScheduler.update(DayOfWeek.MONDAY, 10 -> 12, 20.0)
    thermostatScheduler.update(DayOfWeek.MONDAY, 12 -> 15, 22.0)
    thermostatScheduler.update(DayOfWeek.MONDAY, start -> end, target)
    thermostatScheduler.targetFor(LocalDateTime.of(2024, 2, 5, 8, 0)) shouldBe 19.0
    for i <- start until end do thermostatScheduler.targetFor(LocalDateTime.of(2024, 2, 5, i, 0)) shouldBe target
    for i <- end to 15 do thermostatScheduler.targetFor(LocalDateTime.of(2024, 2, 5, i, 0)) shouldBe 22.0
  }
}
