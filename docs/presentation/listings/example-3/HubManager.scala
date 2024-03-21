/** A concrete hub manager, mocking sources with graphical views. */
class MockedHubManager(using Async.Spawn, AsyncOperations):
  def run(): Unit =
    val channelBySensor = sensorsSource.publishingChannel.groupBy(_.getClass)
    Task:
      channelBySensor.read() match
        case Right((clazz, c)) if clazz == classOf[TemperatureEntry] =>
          thermostatManager.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]])
        case Right((clazz, c)) if clazz == classOf[LuminosityEntry] =>
          lightingManager.run(c.asInstanceOf[ReadableChannel[LuminosityEntry]])
        case _ => ()
    .schedule(RepeatUntilFailure()).start()
    sensorsSource.asRunnable.start().await

trait ThermostatManager extends ThermostatComponent[ThermostatHourlyScheduler] ...:
  /** Run the manager, spawning a new controller consuming the given source of events. */
  def run(source: ReadableChannel[TemperatureEntry])(using Async.Spawn, AsyncOperations) =
    thermostat.asRunnable.start()
    sensorHealthChecker.asRunnable.start()
    Controller.oneToMany(
      publisherChannel = source,
      consumers = Set(thermostat, sensorHealthChecker),
      transformation = _.bufferWithin(samplingWindow),
    ).start()