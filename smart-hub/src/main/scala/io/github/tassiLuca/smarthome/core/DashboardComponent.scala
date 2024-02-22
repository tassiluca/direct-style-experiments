package io.github.tassiLuca.smarthome.core

trait DashboardComponent:
  
  enum HeaterState:
    case ON, OFF
  
  val dashboard: Dashboard
  
  trait Dashboard:
    def updateTemperature(entries: Seq[TemperatureEntry]): Unit
    def newHeaterState(state: HeaterState): Unit
    def newAlert(msg: String): Unit
