package io.github.tassiLuca.smarthome.core

import io.github.tassiLuca.rears.Observable

enum SensorsEvent:
  case TemperatureEntry(temperature: Double)
  case LuminosityEntry(luminosity: Double)

trait SensorSource extends Observable[SensorsEvent]
