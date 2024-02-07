package io.github.tassiLuca.smarthome.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{DayOfWeek, LocalDateTime}

class ThermostatSchedulerTest extends AnyFlatSpec with Matchers {

  private val targetTemperature = 19.5
  private val thermostatScheduler = new ThermostatSchedulerComponent:
    override val scheduler: ThermostatScheduler = ThermostatScheduler(targetTemperature)

  "ThermostatScheduler" should s"be initialized to have a target temperature of $targetTemperature" in {
    thermostatScheduler.scheduler.currentTarget shouldBe targetTemperature
  }

  "ThermostatScheduler when updated" should "return a new ThermostatScheduler with updated schedule" in {
    thermostatScheduler.scheduler.update(DayOfWeek.MONDAY, 9 -> 10, 21.0)
    thermostatScheduler.scheduler.targetFor(LocalDateTime.of(2024, 2, 5, 9, 30)) shouldBe 21.0
  }

  // TODO: check case in which different timespan overlaps
}
