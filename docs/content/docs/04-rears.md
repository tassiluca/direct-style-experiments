# An attempt to bring reactivity principles in gears

So far, we've explored the basics of asynchronous abstraction mechanisms provided by the direct style of the Scala Gears and Kotlin Coroutines frameworks.

The goal of this last example is to investigate, using a simple example, whether these two frameworks offers sufficient idiomatic abstractions to deal with **reactive-like systems**.

## Smart Hub System example

{{< hint info >}}
**Idea**: in an IoT context a multitude of sensors of different types, each replicated to ensure accurate measurements, transmit their measurements to a central hub, which in turns needs to react, in real-time, forwarding to the appropriate controller the data, possibly running some kind of transformation, enabling controllers to make decisions based on their respective logic.
{{< /hint >}}

### Scala Gears version

Before delving into the example, two abstractions of Gears, yet not covered, are introduced:

- `Task`s provide a way, not only to run asynchronous computation, essentially wrapping a `Future`, but also to schedule it, possibly repeating it. Different scheduling strategies are available: `Every`, `ExponentialBackoff`, `FibonacciBackoff`, `RepeatUntilFailure`, `RepeatUntilSuccess`.
  - This allows implementing easily proactive like systems, like a game loop.

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

- To avoid the _work stealing behavior_ of channels consumers, a `ChannelMultiplexer` can be used. It is essentially a container of producing and consuming channels, which can be added and removed at runtime. Internally, it is implemented with a thread that continuously races the set of publishers and once it reads a value, it forwards it to each subscriber channel.
  - Order is guaranteed only per producer;
  - Typically, the consumer creates a channel and adds it to the multiplexer, then start reading from it, possibly using a scheduled task.
    - if the consumer attaches the channel after the producer started, the values sent during this interval are lost, like _hot observables_ in Rx.

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

In the proposed strawman Scala Gears library, there are no other kind of abstractions, neither a way to manipulate channels with functions inspired by Rx.

The attempt was to somehow extend this framework adding first class support for the concept of `Producer` and `Consumer` and implement some of the most common Rx operators, just as a proof of concept, completely leaving out performances concerns.

[Sources can be found in the `rears` submodule]

```scala
/** A publisher, i.e. a runnable active entity producing items on a channel. */
trait Publisher[E]:
  /** The [[Channel]] to send items to. */
  protected val channel: Channel[E] = UnboundedChannel()

  /** @return a runnable [[Task]]. */
  def asRunnable: Task[Unit]

  /** @return a [[ReadableChannel]] where produced items are placed. */
  def publishingChannel: ReadableChannel[E] = channel.asReadable

/** A consumer, i.e. a runnable active entity devoted to consuming data from a channel. */
trait Consumer[E]:
  /** The [[SendableChannel]] to send items to. */
  val listeningChannel: SendableChannel[Try[E]] = UnboundedChannel()

  /** @return a runnable [[Task]]. */
  def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach(react)
  }.schedule(RepeatUntilFailure())

  /** The suspendable reaction triggered upon a new read of an item succeeds. */
  protected def react(e: Try[E])(using Async): Unit

/** A mixin to make consumers stateful. */
trait State[E]:
  consumer: Consumer[E] =>

  private var _state: Option[E] = None

  def state: Option[E] = synchronized(_state)

  override def asRunnable: Task[Unit] = Task {
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach { e =>
      react(e)
      synchronized { _state = e.toOption }
    }
  }.schedule(RepeatUntilFailure())
```

```scala
object Controller:

  def oneToMany[T, R](
      publisherChannel: ReadableChannel[T],
      consumers: Set[Consumer[R]],
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

---

Implemented transformation functions:

- `filter`
- `debounce`
- `groupBy`
- `buffer`
- `bufferWithin`

Pay attention to: Async ?=>

---

Going back to the example, here is presented a schema summarizing the proposed design of the system.

![system design of the example](../../res/img/rears.svg)

---

### Kotlin Coroutines version

## Conclusions
