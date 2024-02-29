---
bookToc: false
---

# An attempt to bring reactivity principles into gears

So far, we've explored the basics of asynchronous abstraction mechanisms provided by the direct style of the Scala Gears and Kotlin Coroutines frameworks.

The goal of this last example is to investigate, using a simple example, whether these two frameworks offer sufficient idiomatic abstractions to deal with **reactive-like systems**.

## Smart Hub System example

{{< hint info >}}
**Idea**: in an IoT context, a multitude of sensors of different types, each replicated to ensure accuracy, transmit their measurements to a central hub, which in turn needs to react, in real-time, forwarding to the appropriate controller the data, possibly running some kind of transformation, enabling controllers to make decisions based on their respective logic.
{{< /hint >}}

### Scala Gears version

Before delving into the example, two abstractions of Gears, yet not covered, are introduced:

- `Task`s provide a way, not only to run asynchronous computation, essentially wrapping a `() => Future[T]`, but also to schedule it, possibly repeating it. Different scheduling strategies are available: `Every`, `ExponentialBackoff`, `FibonacciBackoff`, `RepeatUntilFailure`, `RepeatUntilSuccess`.
  - This allows for implementing easily proactive computations, like a game loop.

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

- if the body of a `Task` **does not** perform any suspending operations the `Async.blocking` blocks the current thread until the task is completed (either successfully or not);
- if the body of a `Task` **does** perform suspending operations then the `Async.blocking` **does not wait for the task to complete** and its context is left as soon as reaches its end.
  - If we want to wait for the task completion, we need to use `Async.await` (or `awaitResult`)
  - **Cons**: depending on the content of the block, the behavior is different! This is _error-prone_ and very difficult to debug with high-order functions:
{{< /hint >}}

- To avoid the _work-stealing behavior_ of channel consumers, a `ChannelMultiplexer` can be used. It is essentially a container of producing and consuming channels, which can be added and removed at runtime. Internally, it is implemented with a thread that continuously races the set of publishers and once it reads a value, it forwards it to each subscriber channel.
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

The attempt was to extend this framework adding first-class support for `Producer` and `Consumer`'s concepts and implementing some of the most common Rx operators, just as a proof of concept, completely leaving out performance concerns.

[Sources can be found in the `rears` submodule](https://github.com/tassiLuca/PPS-22-direct-style-experiments/tree/master/rears/src/main/scala/io/github/tassiLuca/rears).

- A `Producer` is a runnable entity, programmed with a `Task`, producing items on a channel. It exposes the `publishingChannel` method, which returns a `ReadableChannel` through which interested parties can read produced items.
- A `Consumer` is a runnable entity devoted to consuming data from a channel, exposed by the `listeningChannel` method which returns a `SendableChannel`.
  - It can be made stateful by mixing it with the `State` trait, keeping track of its state, updated with the result of the reaction.

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

  /** The [[SendableChannel]] to send items to, where consumers 
    * listen for new items. */
  val listeningChannel: SendableChannel[Try[E]] = UnboundedChannel()

  /** @return a runnable [[Task]]. */
  def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach(react)
  }.schedule(RepeatUntilFailure())

  /** The suspendable reaction triggered upon a new read of an item succeeds. */
  protected def react(e: Try[E])(using Async): S

/** A mixin to make consumer stateful. 
  * Its state is updated with the result of the [[react]]ion.
  * Initially its state is set to [[initialValue]]. */
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

```scala
type PipelineTransformation[T, R] = ReadableChannel[T] => ReadableChannel[R]

object Controller:
  /** Creates a runnable [[Task]] forwarding the items read from the 
    * [[publisherChannel]] to the given [[consumer]], after having it 
    * transformed with the given [[transformation]].
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

  /** Creates a runnable [[Task]] forwarding the items read from the 
    * [[publisherChannel]] to all consumers' channels, after having it 
    * transformed with the given [[transformation]].
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

The following `PipelineTransformation`s have been implemented (inspired by Rx):

```scala
/** @return a new [[ReadableChannel]] whose elements passes the 
  * given predicate [[p]].
  *
  * Example:
  * <pre>
  * ----1---2-------3----4---5--6----7---8---9---10--->
  *     |   |       |    |   |  |    |   |   |   |
  * ----V---V-------V----V---V--V----V---V---V---V-----
  *                 filter(_ % 2 == 0)
  * --------|--------------|------|-------|---------|--
  *         V              V      V       V         V
  * --------2--------------4------6-------8--------10->
  * </pre>
  */
def filter(p: T => Boolean): ReadableChannel[T] = fromNew[T] { c =>
  val value = r.read().toOption.get
  if p(value) then c.send(value)
}
```

```scala
/** @return a new [[ReadableChannel]] whose elements are emitted only after
  *         the given [[timespan]] has elapsed since the last emission.
  *
  * Example:
  * <pre>
  * ----1---2-------3----4---5--6-----7---8---9---10-->
  *     |   |       |    |   |  |     |   |   |   |
  *     V   V       V    V   V  V     V   V   V   V
  * T----------T----------T----------T----------T------
  *                 debounce(1 second)
  * ---------------------------------------------------
  *        |         |         |      |             |
  *        V         V         V      V             V
  * -------1---------3---------5------7------------10->
  * </pre>
  */
def debounce(timespan: Duration): ReadableChannel[T]
```

```scala
/** Groups the items emitted by a [[ReadableChannel]] according to the given 
  * [[keySelector]].
  * @return key-value pairs, where the keys are the set of results obtained 
  *         from applying the [[keySelector]] coupled to a new [[ReadableChannel]] 
  *         where only items belonging to that grouping are emitted.
  *
  * Example:
  * <pre>
  * ----1---2-3--4---5--6--->
  *     |   | |  |   |  |
  *     V   V V  V   V  V
  * -------------------------
  *    groupBy(_ % 2 == 0)
  * -------------------------
  *      \     \
  * ----false--true------------>
  *        1     2
  *         \     \
  *          \     4
  *           3     \
  *            \     \
  *             5     6
  * </pre>
  */
def groupBy[K](keySelector: T => K): ReadableChannel[(K, ReadableChannel[T])]
```

```scala
/** @return a new [[ReadableChannel]] whose elements are buffered in a [[List]] 
  *         of size [[n]]. If [[timespan]] duration is elapsed since last read
  *         the list is emitted with collected elements until that moment.
  *
  * Example:
  * <pre>
  * ----1---2-3----4---5--6----7---8------->
  *     |   | |    |   |  |    |   |
  *     V   V V    V   V  V    V   V
  * ----------------------------------------
  *   buffer(n = 3, timespan = 5 seconds)
  * ----------------------------------T-----
  *         |           |             |
  *         V           V             V
  * ----[1, 2, 3]---[4, 5, 6]------[7, 8]-->
  * </pre>
  */
def buffer(n: Int, timespan: Duration = 5 seconds): ReadableChannel[List[T]]
```

```scala
/** @return a new [[ReadableChannel]] whose elements are buffered in a [[List]] 
  *         of items if emitted within [[timespan]] duration after the first one.
  *
  * Example:
  * <pre>
  * ----1---2-3-4---5--6--7----------8----------->
  *     |   | | |   |  |  |          |
  *     V   V V V   V  V  V          V
  * ----|--------T--|--------T-------|--------T---
  *      buffer(timespan = 5 seconds)
  * -------------|-----------|----------------|---
  *              V           V                V
  * -------[1, 2, 3, 4]--[5, 6, 7]-----------[8]->
  * </pre>  .
  */
def bufferWithin(timespan: Duration = 5 seconds): ReadableChannel[List[T]]
```

Going back to the example here is presented a schema summarizing the proposed design of the system:

![system design of the example](../../res/img/rears.svg)

- two types of sensors: `TemperatureSensor` and `LuminositySensor`;
- sensors send data to the smart hub `SensorSource` (e.g. via MQTT)
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
  - `SensorHealthChecker` is a stateful consumer of generic `SensorEvent`s that checks the health of the sensors, sending alerts in case of malfunctioning:

    ```scala
    /** A [[state]]ful consumer of [[SensorEvent]] detecting possible 
      * malfunctioning and keeping track of last known active sensing units.
      */
    trait SensorHealthChecker extends Consumer[Seq[E], Seq[E]] with State[Seq[E], Seq[E]]
    ```

  - The `Thermostat` is a stateful consumer of temperature entries, taking care of controlling the heating system:
  
    ```scala
    /** A [[state]]ful consumer of [[TemperatureEntry]]s in charge 
      * of controlling the heater and keeping track of the last 
      * detection average temperature.
      */
    trait Thermostat
        extends Consumer[Seq[TemperatureEntry], Option[Temperature]]
        with State[Seq[TemperatureEntry], Option[Temperature]]:
      val scheduler: ThermostatScheduler
    ```

  - `LightingSystem` is a consumer of luminosity entries, taking care of controlling the lighting system;
  
    ```scala
    trait LightingSystem extends Consumer[Seq[LuminosityEntry], Unit]
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

  ```scala
  // LightingManager
  def run(source: ReadableChannel[LuminosityEntry])(using Async, AsyncOperations): Unit =
    lightingSystem.asRunnable.run
    Controller.oneToOne(
      publisherChannel = source,
      consumer = lightingSystem,
      transformation = r => r.bufferWithin(samplingWindow),
    ).run
  ```

### Kotlin Coroutines version

## Conclusions
