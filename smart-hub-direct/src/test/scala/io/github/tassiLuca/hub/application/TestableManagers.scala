package io.github.tassiLuca.hub.application

import gears.async.Async
import io.github.tassiLuca.hub.core.{Luminosity, Temperature}

import scala.concurrent.duration.DurationInt

val samplingWindow = 5.seconds

enum Message:
  case HeaterOn, HeaterOff, Alert, HeaterGetState, TemperatureUpdate, LuminosityUpdate, LightingOff, LightingOn

object TestableLightingManager extends LightingManager:

  var lightingActions: Seq[Message] = Seq.empty
  var dashboardMessages: Seq[Message] = Seq.empty

  override val dashboard: DashboardService = new DashboardService:
    override def offHeaterNotified(): Unit =
      dashboardMessages = dashboardMessages :+ Message.HeaterOff
    override def onHeaterNotified(): Unit =
      dashboardMessages = dashboardMessages :+ Message.HeaterOn
    override def alertNotified(message: String): Unit =
      dashboardMessages = dashboardMessages :+ Message.Alert
    override def temperatureUpdated(temperature: Temperature): Unit =
      dashboardMessages = dashboardMessages :+ Message.TemperatureUpdate
    override def luminosityUpdate(luminosity: Luminosity): Unit =
      dashboardMessages = dashboardMessages :+ Message.LuminosityUpdate
    override def updateSchedule(schedule: Map[(String, String), String]): Unit = ()

  override val lamps: LampsController = new LampsController:
    override def off()(using Async): Unit =
      lightingActions = lightingActions :+ Message.LightingOff
    override def on()(using Async): Unit =
      lightingActions = lightingActions :+ Message.LightingOn

object TestableThermostatManager extends ThermostatManager:

  var alerts: Seq[Message] = Seq.empty
  var dashboardMessages: Seq[Message] = Seq.empty
  var heaterActions: Seq[Message] = Seq.empty

  override val alertSystem: AlertSystem = new AlertSystem:
    override def notify(message: String)(using Async): Unit =
      alerts = alerts :+ Message.Alert

  override val dashboard: DashboardService = new DashboardService:
    override def offHeaterNotified(): Unit =
      dashboardMessages = dashboardMessages :+ Message.HeaterOff
    override def onHeaterNotified(): Unit =
      dashboardMessages = dashboardMessages :+ Message.HeaterOn
    override def alertNotified(message: String): Unit =
      dashboardMessages = dashboardMessages :+ Message.Alert
    override def temperatureUpdated(temperature: Temperature): Unit =
      dashboardMessages = dashboardMessages :+ Message.TemperatureUpdate
    override def luminosityUpdate(luminosity: Luminosity): Unit =
      dashboardMessages = dashboardMessages :+ Message.LuminosityUpdate
    override def updateSchedule(schedule: Map[(String, String), String]): Unit = ()

  override val heater: Heater = new Heater:
    override def on()(using Async): Unit =
      heaterActions = heaterActions :+ Message.HeaterOn
    override def off()(using Async): Unit =
      heaterActions = heaterActions :+ Message.HeaterOff
    override def state: HeaterState =
      heaterActions = heaterActions :+ Message.HeaterGetState
      HeaterState.OFF
