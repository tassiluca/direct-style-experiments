---
bookToc: false
---

# Reactivity in direct style

So far, we've explored the basics of asynchronous abstraction mechanisms provided by the direct style of the Scala Gears and Kotlin Coroutines frameworks.
The goal of this last example is to investigate, using a simple example, whether these two frameworks offer sufficient idiomatic abstractions to deal with event-based reactive systems.

## Smart Hub example

{{< hint info >}}
**Idea**: in an IoT context, a multitude of sensors of different types, each replicated to ensure accuracy, transmit their measurements to a central hub, which in turn needs to react, in real-time, forwarding to the appropriate controller the data, possibly performing some kind of transformation.
{{< /hint >}}

### Scala Gears version

Before delving into the example, two abstractions of Gears, yet not covered, are introduced:

- `Task`s provide a way, not only to run asynchronous computation, essentially wrapping a `() => Future[T]`, but also to schedule it, possibly repeating it. Different scheduling strategies are available: `Every`, `ExponentialBackoff`, `FibonacciBackoff`, `RepeatUntilFailure`, `RepeatUntilSuccess`.
  - This allows for implementing easily proactive computations

{{< mermaid class="smaller" >}}
classDiagram
  class `Task[+T]` {
    +apply(body: (Async, AsyncOperations) ?=> T) Task[T]$
    +run(using Async, AsyncOperations) Future[+T]
    +schedule(s: TaskSchedule) Task[T]
  }

  `Task[+T]` o--> TaskSchedule

  class TaskSchedule {
    << enum >>
    + Every(millis: Long, maxRepetitions: Long = 0)
    + ExponentialBackoff(millis: Long, exponentialBase: Int = 2, maxRepetitions: Long = 0)
    + FibonacciBackoff(millis: Long, maxRepetitions: Long = 0)
    + RepeatUntilFailure(millis: Long = 0, maxRepetitions: Long = 0)
    + RepeatUntilSuccess(millis: Long = 0, maxRepetitions: Long = 0)
  }
{{< /mermaid >}}

{{< hint warning >}}
**Warning**: when `Task`s are scheduled with `RepeatUntil*`:

- if the body of a `Task` **does not** perform any suspending operations the `Async.blocking` **blocks** the current thread **until the task is completed** (either successfully or not);
- if the body of a `Task` **does** perform suspending operations then the `Async.blocking` **does not wait for the task to complete** and its context is left as soon as reaches its end.
  - If we want to wait for the task completion, it's the client's responsibility to explicitly `Async.await` (or `awaitResult`)
  - **Cons**: depending on the content of the block, the behavior is different! This is _error-prone_!
{{< /hint >}}

{{< hint warning >}}

**Warning**: with high-order functions if we deal with repeated `Tasks`, in some cases an `Async ?=>` label is required to not suspend the whole block, even if a suspending operation is performed: the code below *behaves differently* if the `Async ?=>` label is present or not. Note: this may be an unintended effect of the library, yet to be investigated.

{{< /hint >}}

{{< columns >}}

In this case despite we suspend to wait for the timer tick, the `Async.blocking` blocks until the `Task` is completed.

```scala
Async.blocking:
  val timer = Timer(2.seconds)
  Future(timer.run())
  produce { _ =>
    timer.src.awaitResult // SUSPENDING OPERATION!
    // ...
  }

def produce[T](
  action: SendableChannel[T] => Try[Unit]
)(using Async): ReadableChannel[T] =
  val channel = UnboundedChannel[T]()
  Task {
    action(channel.asSendable)
  }.schedule(RepeatUntilFailure()).run
  channel.asReadable
```

<--->

With the `Async ?=>` label, the `Async.blocking` does not wait for the `Task` to complete!

```scala
Async.blocking:
  val timer = Timer(2.seconds)
  Future(timer.run())
  produceWithLabel { _ =>
    timer.src.awaitResult // SUSPENDING OPERATION!
    // ....
  }

def produceWithLabel[T](
  action: Async ?=> SendableChannel[T] => Try[Unit]
)(using Async): ReadableChannel[T] =
  val channel = UnboundedChannel[T]()
  Task {
    action(channel.asSendable)
  }.schedule(RepeatUntilFailure()).run
  channel.asReadable
```

{{< /columns >}}

[[See the tests for more details.](https://github.com/tassiLuca/PPS-22-direct-style-experiments/blob/master/commons/src/test/scala/io/github/tassiLuca/TasksTest.scala#L27)]

- To avoid the _work-stealing behavior_ of channel consumers, a `ChannelMultiplexer` can be used. It is essentially a container of `Readable` and `Sendable` channels, which can be added and removed at runtime. Internally, it is implemented with a thread that continuously races the set of publishers and once it reads a value, it forwards it to each subscriber channel.
  - Order is guaranteed only per producer;
  - Typically, the consumer creates a channel and adds it to the multiplexer, then starts reading from it, possibly using a scheduled task.
    - if the consumer attaches to the channel after the producer has started, the values sent during this interval are lost, like _hot observables_ in Rx.

{{< mermaid class="smallest" >}}
classDiagram
  namespace javaio {
    class Closeable {
      << interface >>
      +close()
    }
  }

  class `ChannelMultiplexer[T]` {
    << trait >>
    +run()(using Async)
    +addPublisher(c: ReadableChannel[T])
    +removePublisher(c: ReadableChannel[T])
    +addSubscriber(c: SendableChannel[Try[T]])
    +removeSubscriber(c: SendableChannel[Try[T]])
  }

  Closeable <|-- `ChannelMultiplexer[T]`
{{< /mermaid >}}

In the proposed strawman Scala Gears library, there are no other kinds of abstractions, nor a way to manipulate channels with functions inspired by Rx.

The attempt, described in the following, has been to extend this framework adding first-class support for `Producer` and `Consumer`'s concepts and implementing some of the most common Rx operators, completely leaving out performance concerns.

[[Sources can be found in the `rears` submodule.]](https://github.com/tassiLuca/PPS-22-direct-style-experiments/tree/master/rears/src/main/scala/io/github/tassiLuca/rears)

- A `Producer` is a runnable entity, programmed with a `Task`, producing items on a channel. It exposes the `publishingChannel` method, which returns a `ReadableChannel` through which interested consumers can read produced items.
- A `Consumer` is a runnable entity devoted to consuming data from a channel, exposed by the `listeningChannel` method which returns a `SendableChannel` to send items to.
  - It can be made stateful by mixing it with the `State` trait, allowing it to keep track of its state, which is updated every time with the result of the `react`ion (i.e. its return type).
  - **Warning**: like in an event-loop, the `react`ion logic should not perform long-lasting blocking operation, otherwise, the whole system will not react to new events.

```scala
/** A producer, i.e. a runnable entity producing items on a channel. */
trait Producer[E]:
  /** The [[Channel]] where specific [[Producer]]s send items to. */
  protected val channel: Channel[E] = UnboundedChannel()

  /** @return the publisher's behavior encoded as a runnable [[Task]]. */
  def asRunnable: Task[Unit]

  /** @return the [[ReadableChannel]] where produced items are placed. */
  def publishingChannel: ReadableChannel[E] = channel.asReadable

/** A consumer, i.e. a runnable entity devoted to consume data from a channel. */
trait Consumer[E, S]:

  /** The [[SendableChannel]] to send items to, where the consumer listen for new items. */
  val listeningChannel: SendableChannel[Try[E]] = UnboundedChannel()

  /** @return a runnable [[Task]]. */
  def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach(react)
  }.schedule(RepeatUntilFailure())

  /** The suspendable reaction triggered upon a new read of an item succeeds. */
  protected def react(e: Try[E])(using Async): S

/** A mixin to make consumer stateful. Its state is updated with the result
  * of the [[react]]ion. Initially its state is set to [[initialValue]].
  */
trait State[E, S](initialValue: S):
  consumer: Consumer[E, S] =>

  private var _state: S = initialValue

  /** @return the current state of the consumer. */
  def state: S = synchronized(_state)

  override def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach { e =>
      synchronized { _state = react(e) }
    }
  }.schedule(RepeatUntilFailure())
```

- The `Controller` object exposes methods wiring `Producer` and `Consumer`s altogether, possibly performing some kind of transformation on the `publisherChannel`.
  - the `oneToOne` method just wires one single consumer to the `publisherChannel` given in input, possibly having it transformed with the provided transformation.
  - the `oneToMany` allows many consumers to be wired to the `publisherChannel`, possibly having it transformed.
    - to accomplish this, a `ChannelMultiplexer` is used, which is in charge of forwarding the items read from the transformed `publisherChannel` to all consumers' channels.

```scala
/** Simply, a function that, given in input a [[ReadableChannel]], performs some 
  * kind of transformation, returning, as a result, another [[ReadableChannel]]. */
type PipelineTransformation[T, R] = ReadableChannel[T] => ReadableChannel[R]

object Controller:
  /** Creates a runnable [[Task]] forwarding the items read from the [[publisherChannel]] 
    * to the given [[consumer]], after having it transformed with the given [[transformation]].
    */
  def oneToOne[T, R](
      publisherChannel: ReadableChannel[T],
      consumer: Consumer[R, ?],
      transformation: PipelineTransformation[T, R] = identity,
  ): Task[Unit] =
    val transformedChannel = transformation(publisherChannel)
    Task {
      consumer.listeningChannel.send(transformedChannel.read().tryable)
    }.schedule(RepeatUntilFailure())

  /** Creates a runnable [[Task]] forwarding the items read from the [[publisherChannel]] to 
    * all consumers' channels, after having it transformed with the given [[transformation]].
    */
  def oneToMany[T, R](
      publisherChannel: ReadableChannel[T],
      consumers: Set[Consumer[R, ?]],
      transformation: PipelineTransformation[T, R] = identity,
  ): Task[Unit] = Task:
    val multiplexer = ChannelMultiplexer[R]()
    consumers.foreach(c => multiplexer.addSubscriber(c.listeningChannel))
    multiplexer.addPublisher(transformation(publisherChannel))
    // blocking call: the virtual thread on top of which this task is 
    // executed needs to block to continue publishing publisher's events 
    // towards the consumer by means of the multiplexer.
    multiplexer.run()
```

The following `PipelineTransformation`s have been implemented (inspired by Rx). [Tests in `rears` submodule](https://github.com/tassiLuca/PPS-22-direct-style-experiments/blob/master/rears/src/test/scala/io/github/tassiLuca/rears/PipelineTransformationsTest.scala) provide the necessary examples to understand their behavior.

#### Filter

```scala
/** @return a new [[ReadableChannel]] whose elements passes the given predicate [[p]].  */
def filter(p: T => Boolean): ReadableChannel[T]
```

Example:

```
----1---2-------3----4---5--6----7---8---9---10--->
    |   |       |    |   |  |    |   |   |   |
----V---V-------V----V---V--V----V---V---V---V-----
                filter(_ % 2 == 0)
--------|--------------|------|-------|---------|--
        V              V      V       V         V
--------2--------------4------6-------8--------10->
```

#### Map

```scala
/** @return a new [[ReadableChannel]] whose values are transformed accordingly to the given function [[f]]. */
def map[R](f: T => R): ReadableChannel[R]
```

Example:

```
----1---2-------3----4---5------6--------7-------->
    |   |       |    |   |      |        |
----V---V-------V----V---V------V--------V---------
                map(x => x * x)
----|---|-------|----|---|------|--------|---------
    V   V       V    V   V      V        V
----1---4-------9----16--25-----36-------49------->
```

#### Debounce

```scala
/** @return a new [[ReadableChannel]] whose elements are emitted only after
  *         the given [[timespan]] has elapsed since the last emission. */
def debounce(timespan: Duration): ReadableChannel[T]
```

Example:

```
----1---2-------3----4---5--6-----7---8---9---10-->
    |   |       |    |   |  |     |   |   |   |
    V   V       V    V   V  V     V   V   V   V
T----------T----------T----------T----------T------
                debounce(1 second)
---------------------------------------------------
       |         |         |      |             |
       V         V         V      V             V
-------1---------3---------5------7------------10->
```

#### GroupBy

```scala
/** Groups the items emitted by a [[ReadableChannel]] according to the given [[keySelector]].
  * @return key-value pairs, where the keys are the set of results obtained from applying the
  *         [[keySelector]] coupled to a new [[ReadableChannel]] where only items belonging to
  *         that grouping are emitted.
  */
def groupBy[K](keySelector: T => K): ReadableChannel[(K, ReadableChannel[T])]
```

Example:

```
----1---2-3--4---5--6--->
    |   | |  |   |  |
    V   V V  V   V  V
-------------------------
       groupBy(_ % 2)
-------------------------
     \     \
----false--true------------>
       1     2
        \     \
         \     4
          3     \
           \     \
            5     6
```

#### Buffer

```scala
/** @return a new [[ReadableChannel]] whose elements are buffered in a [[List]] of size [[n]].
  *         If [[timespan]] duration is elapsed since last read the list is emitted
  *         with collected elements until that moment (default: 5 seconds).
  */
def buffer(n: Int, timespan: Duration = 5 seconds): ReadableChannel[List[T]]
```

Example:

```
----1---2-3----4---5--6----7---8-------->
    |   | |    |   |  |    |   |
    V   V V    V   V  V    V   V
|---------|-----------|------------T-----
   buffer(n = 3, timespan = 5 seconds)
|---------|-----------|------------|-----
          V           V            V
------[1, 2, 3]---[4, 5, 6]------[7, 8]->
```

#### BufferWithin

```scala
/** @return a new [[ReadableChannel]] whose elements are buffered in a [[List]] of items
  *         if emitted within [[timespan]] duration after the first one (default: 5 seconds).
  */
def bufferWithin(timespan: Duration = 5 seconds): ReadableChannel[List[T]]
```

Example:

```
----1---2-3-4---5--6--7----------8----------->
    |   | | |   |  |  |          |
    V   V V V   V  V  V          V
----|--------T--|--------T-------|--------T---
         buffer(timespan = 5 seconds)
-------------|-----------|----------------|---
             V           V                V
-------[1, 2, 3, 4]--[5, 6, 7]-----------[8]->
```

Going back to the example here is presented a schema summarizing the flows of data and the transformations to apply to them. This is just a simple example used to test the proposed abstractions.

{{< figure src="../../res/img/rears.svg" alt="System design of the example" width="90%" class="center" >}}

[[Sources are available in `smart-hub-direct` submodule](https://github.com/tassiLuca/PPS-22-direct-style-experiments/tree/master/smart-hub-direct/src/main/scala/io/github/tassiLuca/hub)].

- For simplicity, two types of sensors are considered: `TemperatureSensor` and `LuminositySensor`;
- sensors send data to the smart hub `SensorSource` (e.g., in a real case scenario, via MQTT)
  - `SensorSource` is a `Producer[SensorEvent]`, publishing received data on its `publishingChannel`:

    ```scala
    trait SensorSource extends Producer[SensorEvent]
    
    sealed trait SensorEvent(val name: String)
    case class TemperatureEntry(sensorName: String, temperature: Temperature) 
      extends SensorEvent(sensorName)
    case class LuminosityEntry(sensorName: String, luminosity: Temperature) 
      extends SensorEvent(sensorName)
    ```

- three main controllers:
  - `SensorHealthChecker` is a stateful consumer of generic `SensorEvent`s that checks the health of the sensors, sending alerts in case of malfunctioning. Here the state is necessary to determine the health of the sensors, based on the last detection.

    ```scala
    /** A [[state]]ful consumer of [[SensorEvent]] detecting possible 
      * malfunctioning and keeping track of last known active sensing units.
      */
    trait SensorHealthChecker extends Consumer[Seq[E], Seq[E]] with State[Seq[E], Seq[E]]
    ```

  - The `Thermostat` is a stateful consumer of temperature entries, taking care of controlling the heating system. The fact the thermostat keeps track of the last average detection could be useful to a ReSTful API, for example.

  
    ```scala
    /** A [[state]]ful consumer of [[TemperatureEntry]]s in charge of controlling
      * the heater and keeping track of the last detected average temperature.
      */
    trait Thermostat
        extends Consumer[Seq[TemperatureEntry], Option[Temperature]]
        with State[Seq[TemperatureEntry], Option[Temperature]]:
      val scheduler: T
    ```

  - `LightingSystem` is a basic consumer (non-stateful) of luminosity entries, taking care of controlling the lighting system.
  
    ```scala
    /** A consumer of [[LuminosityEntry]], in charge of controlling the lighting system. */
    trait LightingSystem extends Consumer[Seq[LuminosityEntry], Unit]
    ```

  Each of these controllers reacts to the data received based on their logic and their actual state to accomplish a specific task.
  For example:

    - the sensor checker sends alerts whether, compared with the previous detection, it did not receive data from some sensors:

      ```scala
      override protected def react(e: Try[Seq[E]])(using Async): Seq[E] = e match
        case Success(current) =>
          val noMoreActive = state.map(_.name).toSet -- current.map(_.name).toSet
          if noMoreActive.nonEmpty then
            sendAlert(s"[${currentTime}] Detected ${noMoreActive.mkString(", ")} no more active!")
          current
        case Failure(es) => sendAlert(es.getMessage); Seq()
      ```

    - the thermostat computes the average temperature and, based on a scheduler, decides whether to turn on or off the heating system:

      ```scala
      override protected def react(e: Try[Seq[TemperatureEntry]])(using Async): Option[Temperature] =
        for
          averageTemperature <- e.map { entries => entries.map(_.temperature).sum / entries.size }.toOption
          _ = averageTemperature.evaluate() // here logic to decide whether turn on or off heating system
        yield averageTemperature
      ```

- The `HubManager` takes care of grouping sensor data by their type and forwarding them to the appropriate manager, either `ThermostatManager` or `LightingManager`:

  ```scala
  val channelBySensor = sensorsSource.publishingChannel.groupBy(_.getClass)
  Task {
    channelBySensor.read() match
      case Right((clazz, c)) if clazz == classOf[TemperatureEntry] =>
        thermostatManager.run(c.asInstanceOf[ReadableChannel[TemperatureEntry]])
      case Right((clazz, c)) if clazz == classOf[LuminosityEntry] =>
        lightingManager.run(c.asInstanceOf[ReadableChannel[LuminosityEntry]])
      case _ => ()
  }.schedule(RepeatUntilFailure()).run
  sensorsSource.asRunnable.run.await
  ```

- Both `ThermostatManager` and `LightingManager` are in charge of creating the appropriate `Controller` instance, based on the number of `Consumer`s and pipeline transformation we need to implement:

  ```scala
  // ThermostatManager
  def run(source: ReadableChannel[TemperatureEntry])(using Async, AsyncOperations): Unit =
    thermostat.asRunnable.run
    sensorHealthChecker.asRunnable.run
    Controller.oneToMany(
      publisherChannel = source,
      consumers = Set(thermostat, sensorHealthChecker),
      transformation = r => r.bufferWithin(samplingWindow),
    ).run
  ```

To produce a testable version of this example, a simulated source of sensor data has been created, backed to a GUI, through which the user can simulate the behavior of the sensors.
The example is runnable via:

```bash
./gradlew smart-hub-direct:run
```

Three panels should pop up, one for each sensor type, and a dashboard showing the state of the system.
Entering some value in the panels and pressing the "Send" button, after 5 seconds (the configured sampling window), the system should react to the data received, updating the dashboard with the new state.

{{< figure src="../../res/img/smart-hub.png" alt="Smart Hub application" width="90%" >}}

### Kotlin Coroutines version

Kotlin Coroutines offers two other abstractions to deal with asynchronous data streams, belonging to the `flow` "family", which are: `SharedFlow` and `StateFlow`.
Despite their names including `flow`, which we've seen are cold streams, they are actually **hot** (the terminology is a bit misleading...):

- `SharedFlow` is a hot flow that allows for multiple collectors to subscribe to it, enabling the broadcasting of values to multiple consumers or having multiple consumers be "attached" to the same stream of data.
  - they can be configured to buffer a certain number of previously emitted values for new collectors so that they can catch up with the latest values -- the so-called, `replay` cache;
- `StateFlow` is an extension of the `SharedFlow`: it is a hot flow that maintains a single value representing a state, holding one value at a time. It operates as a conflated flow, meaning that when a new value is emitted, it replaces the previous value and is immediately sent to new collectors
  - this type of flow is beneficial for maintaining a single source of truth for a state and automatically updating all collectors with the latest state (for example in ViewModels in Android applications)

In our example, `SharedFlow` is used to model the flow of sensor data:

```kotlin
interface SensorSource<out E : SensorEvent> {
    /** The flow of sensor events. */
    val sensorEvents: SharedFlow<E>
}
```

Like all flows, they have all the kinds of operators presented in the previous example. Despite this, they do not support, at the moment, all the operators that Rx offers, like `groupBy`, `buffer` (in the Rx conception), etc... (even if some proposals are pending to add them in the framework).

For this reason, the consumer of events has been implemented manually, using a loop that, every `samplingWindow` time, reacts to the data received, updating the state of the system. 
By the way, if this solution appears to be less elegant, since `Flow`s are, de facto, the porting of Rx's `Observable's` into the Coroutines world, [libraries exists to convert them to `Observable` and vice versa](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-rx2/).
This could offer (in some cases and where necessary) a way to use the full power of Rx operator, if needed.

```kotlin
/** A consumer of sensor events. */
interface SensorSourceConsumer<in E : SensorEvent, out S> {

    /** The current state of the source consumer. */
    val state: S

    /** Reacts to a sensor event. */
    suspend fun react(e: E)
}

/** A scheduled consumer of sensor events. */
interface ScheduledConsumer<in E : SensorEvent, out S> : SensorSourceConsumer<E, S>, CoroutineScope {

    /** The interval period. */
    val samplingWindow: Duration

    /** The update logic of the consumer. */
    suspend fun update()

    /** Runs the consumer scheduler. */
    fun run() = launch {
        while (true) {
            update()
            delay(samplingWindow)
        }
    }
}
```

The managers just take care of collecting the data and forwarding it to the appropriate consumer. For example, the `ThermostatManager`:

```kotlin
suspend fun run(sensorSource: Flow<TemperatureEntry>) {
    thermostat.run()
    temperatureSensorsChecker.run()
    sensorSource.collect {
        thermostat.react(it)
        temperatureSensorsChecker.react(it)
    }
}
```

## Takeaways

- Channels in Scala Gears are fine to model flow of data **that exist without application's request from them**: incoming network connections, event streams, etc...
- 

{{< button href="https://tassiluca.github.io/PPS-22-direct-style-experiments/PPS-22-direct-style-experiments/docs/03-channels" >}} **Previous**: Channels as a communication primitive{{< /button >}}

{{< button href="https://tassiluca.github.io/PPS-22-direct-style-experiments/PPS-22-direct-style-experiments/docs/05-conclusions" >}} **Next**: Conclusions{{< /button >}}
