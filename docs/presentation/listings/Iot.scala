type Temperature = Double
type Luminosity = Double

/** A generic source of [[SensorEvent]] (e.g. a MQTT broker). */
trait SensorSource extends Producer[SensorEvent]

/** A detection performed by a sensing unit. */
sealed trait SensorEvent(val name: String)
case class TemperatureEntry(sensorName: String, temperature: Temperature) extends SensorEvent(sensorName)
case class LuminosityEntry(sensorName: String, luminosity: Temperature) extends SensorEvent(sensorName)
    