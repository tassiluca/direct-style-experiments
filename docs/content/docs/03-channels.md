# Channels as a communication primitive

The fourth, yet not mentioned, abstraction of both Kotlin Coroutines and Scala Gears is the **channel**.
Channels represents the primitive communication and coordination mean to exchange `Future`s results. They are, at least conceptually, very similar to a queue where it is possible to send (and receive) data (_producer-consumer_ pattern).

{{< mermaid >}}
classDiagram
  class `SendableChannel[-T]` {
    << trait >>
    +sendSource(x: T) Async.Source[Either[Closed, Unit]]
    +send(x: T)(using Async) Unit
  }

  class `ReadableChannel[+T]` {
    << trait >>
    +readSource Async.Source[Either[Closed, T]]
    +read()(using Async) Either[Closed, T]
  }

  class `Channel[T]` {
    << trait >>
    +asSendable: SendableChannel[T]
    +asReadable: ReadableChannel[T]
    +asCloseable: java.io.Closeable
  }

  namespace java io {
    class Closeable {
      << interface >>
      +close()
    }
  }

  `SendableChannel[-T]` <|-- `Channel[T]`
  Closeable <|-- `Channel[T]`
  `ReadableChannel[+T]` <|-- `Channel[T]`
{{< /mermaid >}}

The channel is defined through three distinct interfaces: `SendableChannel`, `ReadableChannel` and `Channel`, where the latter extends from both `SendChannel` and `ReceiveChannel`. Typically a `Channel` is created and a `SendableChannel` and `ReadableChannel` instances are respectively provided to producer and consumer, restricting their access to it. Same identical design is present in Kotlin Coroutines.

Moreover, `Channel` inherits from `java.io.Closable`, making them closable objects: once closed, they raises `ChannelClosedException` when attempting to write to it and immediately returns a `Failure(ChannelClosedException)` when attempting to read from it.

Three types of channel exists:

- **Synchronous Channels**: links a read request with a send request within a _rendezvous_
  - `send` (`send`) suspend the process until a consumer `read` (`send`) the value;
- **Buffered Channels**: a version of a channel with an internal buffer of fixed size
  - `send` suspend the producer process if it is full; otherwise it appends the value to the buffer, returning immediately;
  - `read` suspend if the channel is empty, waiting for a new value.
- **Unbounded Channels**: a version of a channel with an unbounded buffer
  - if the programs run out of memory you can get out of memory exceptions!

> Multiple producers can send data to the channel, as well as multiple consumers can read them, **but each element is handled only _once_ by _one_ of them**, i.e. consumers **compete** with each other for sent values. Once the element is handled, it is immediately removed from the channel.

## Organization analyzer example

To show channels in action an example has been prepared:

{{< hint info >}}
**Idea**: we want to collect statistics about repositories and contributors of a given GitHub organization. More in details, given a GitHub organization, we want to list all contributors (along with the total amount of contributions) and repositories (along with its most important information, like number of stars, issues and last release).
{{< /hint >}}

As usual, the example has been implemented using monadic `Future`s, as well as Scala gears and Kotlin Coroutines.

Things to do:

1. Get all the repositories of a given organization
2. For each of them:
   * find all the contributors
   * get the last release
3. Merge results


---

- Channels in Kotlin (w.r.t. gears)
  - fairness (also in Gears?)
  - pipeline (not supported in Gears)
  - better closable

---