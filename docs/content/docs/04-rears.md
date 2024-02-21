# An attempt to bring reactivity principles in gears

{{< mermaid >}}
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

{{< mermaid >}}
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

![expected result](../../res/img/rears.svg)

---

Gears:

- Task scheduling
  - Pro and cons
  - proactiveness
  - `Producer` + `Consumer` design
- Manipulation of channels with functions inspired by Rx

Kotlin:

- flows

---
