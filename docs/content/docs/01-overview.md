# Overview of the project

## Context

In the realm of asynchronous programming, the Scala ecosystem offers a set of solid and widely adopted monadic constructs and libraries to tackle complex tasks functionally with elegance and efficiency, like [Monix Tasks](https://monix.io/docs/current/eval/task.html) and [Cats Effecs](https://typelevel.org/cats-effect/), enabling a wide range of interesting and useful features, like composable error handling, cancellation mechanisms and structured concurrency that the standard library lacks.
However, they also come with a cost: the pervasiveness of the `flatMap` operator to compose values makes the code harder to reason about and difficult and awkward to integrate with regular control structures.

In the last years, we have been assisting the increase in adoption of continuation and coroutines in modern runtimes, either exploiting some kind of fibers support, like the project Loom with Virtual Threads, or via code generation, like Kotlin Coroutines, aiming to capture the essence of effects more cleanly compared to monads.

## Goals

The goal of this project is to explore, mainly focusing on Scala, the direct style, developing a few examples (not too complex) leveraging the new strawman library [Gears](https://github.com/lampepfl/gears), comparing it with [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and the current implementation of monadic Futures, seeking to analyze aspects such as:

- ergonomics of the two styles;
- which of the two approaches has a real advantage in adoption;
- pros and cons of the two styles;
- any limitations and difficulties encountered in using them.

## The contribution

The project is built around three small examples.

The [first](../03-basics) aims, through the implementation of the core of a small Web service, to introduce the basics of asynchronous programming in direct style, focusing on structured concurrency and cancellation.

The [second](../04-channels) introduces the main communication and synchronization primitive of direct style, channels, and how they can be used to exchange data between concurrent tasks (either `Future`s or coroutines).

In this context, a contribution has been made by proposing the extension of the Scala Gears library with the following two abstractions, currently missing, inspired by those of the Kotlin Coroutines:

- *terminable channels*, i.e. a channel that can be terminated, but whose values can still be read by consumers after its termination until all values are consumed;
- *`Flow`s*, modeling a *cold* stream of asynchronously computed values which are particularly useful for implementing asynchronous functions that produce, not just a single, but a sequence of values.

The [last example](../05-rears) investigates how to implement a reactive-like event-based system using direct style, taking as a use case a small sensor control system in an IoT context that needs to react to events coming from different sensors.

To accomplish this, since Scala Gears lacks any kind of transforming operator, some have been introduced on top of channels (taking cues from Reactive frameworks) allowing them to be manipulated functionally.

## Conclusions

The current implementation of monadic Futures present in the standard library is insufficient to handle modern asynchronous programming in a structured and safe way: it lacks structured concurrency and cancellation mechanism, is not referential transparent and requires to be ugly mixed with direct style constructs to be used appropriately.

This leads to the adoption of effectful libraries that offer these and many other powerful abstractions, beautifully wrapped in monadic constructs.
This is the main pro and con of the monadic approach: what makes monads remarkable is their capability to turn statements into programmable values and introduce constructs to transform and compose them functionally in a very elegant (and somehow "magical" for ones who are not familiar with them) way.
Despite this idyllic beauty, monads and effectful libraries require relevant expertise in functional programming to be fully grasped and effectively composed.

Direct style frameworks are indeed arising as a more natural and intuitive way to handle concurrency, leveraging an imperative-like programming style that is familiar to all developers.

In the JVM ecosystem, the most adopted and known direct-style library is the Kotlin Coroutines, which were introduced in 2018 by modifying the Kotlin language (rather than its runtime, like the project Loom has recently done with Virtual Thread) to support suspending functions.
The main advantages of Kotlin Coroutines are that they provide suspension and cancellation mechanisms that are simple to understand and use, as well as a good ecosystem for channels and `Flow`s.
Despite this, Coroutines are not still perfect: due to their design, they partially suffer from the [colored functions problem](https://journal.stuffwithstuff.com/2015/02/01/what-color-is-your-function/) and we need to be aware we can not use the same synchronization concurrency abstractions that we would use in Java with threads (like locks, `synchronized`, ...) cause they are not designed to be used in the coroutines context.

Scala Gears is an attempt to bring direct style into the Scala ecosystem.
Its API design is inspired by Kotlin Coroutines and, despite the fact it achieves suspension, unlike Kotlin, leveraging Virtual Threads for JVM and delimited continuation for Scala Native, the majority of constructs can be mapped to the Kotlin Coroutines ones.
Despite being a very young project, it already offers a good set of abstractions for asynchronous programming, although it cannot yet be considered a mature library ready to be used in a production environment:

- some design choices should be addressed:
  - closing a channel prevents any further reading, precluding the possibility of processing the remaining values (see [second](../04-channels) example);
  - Task scheduling behaves differently with higher-order functions depending on its signature and wither or not the function passed is suspendable (see [third](../05-rears) example);
- the library is still missing some important abstractions, like the proposed `Flow` for handling a cold stream of asynchronously computed values, and operators for functionally transforming channels (and in a next future, hopefully, `Flow`s or equivalent abstraction);
- performances: the project has been created for experimenting, thus performances have not been considered a priority so far, [even though a comparison in overheads of
the core primitives has been published](https://github.com/lampepfl/gears/blob/main/docs/summary-2023-06.md#performance).

In conclusion, Scala Gears is a promising project that could bring direct-style async programming into the Scala ecosystem, giving, together with boundary and break Scala 3 support, a nice alternative to the current monadic approach, simplifying the way we handle concurrency, making it more natural and intuitive.

## References

- [Scala Gears, Programming Methods Laboratory EPFL](https://github.com/lampepfl/gears/tree/main/docs)
- [Scala 3: What Is "Direct Style" by D. Wampler](https://medium.com/scala-3/scala-3-what-is-direct-style-d9c1bcb1f810#:~:text=Dean%20Wampler-,Scala%203,without%20the%20boilerplate%20of%20monads.)
- [Kotlin Coroutines documentation](https://kotlinlang.org/docs/coroutines-overview.html)
- [Pre-SIP: Suspended functions and continuations in Scala 3](https://contributors.scala-lang.org/t/pre-sip-suspended-functions-and-continuations/5801/20?u=adamw)
- [The Great Concurrency Smackdown: ZIO versus JDK by John A. De Goes](https://www.youtube.com/watch?v=9I2xoQVzrhs)
- [Continuaton, coroutine, and generator by A. Ber](https://medium.com/geekculture/continuation-coroutine-continuation-generator-9a1af03a3bed)
- [KotlinConf 2017 - Introduction to Coroutines by Roman Elizarov](https://www.youtube.com/watch?v=_hfBv0a09Jc)
- [KotlinConf 2017 - Deep Dive into Coroutines on JVM by Roman Elizarov](https://www.youtube.com/watch?v=YrrUCSi72E8&t=42s)
- [Kotlin Co-routine Scope by A. Nadiger](https://www.linkedin.com/pulse/kotlin-co-routine-scope-amit-nadiger#:~:text=CoroutineScope%20is%20an%20interface%20in,of%20the%20CoroutineScope%20have%20completed.)
- [What Color is your function by B. Nystrom](https://journal.stuffwithstuff.com/2015/02/01/what-color-is-your-function/)
- [Channel in Kotlin Coroutines](https://kt.academy/article/cc-channel)
- [SharedFlow and StateFlow](https://kt.academy/article/cc-sharedflow-stateflow)
- [Demystifying Kotlin's Channel Flows by S. Cooper](https://betterprogramming.pub/demystifying-kotlins-channel-flows-b9007e1f773b)

{{< button relref="/" >}} **Home** {{< /button >}}

{{< button relref="/02-boundaries" >}} **Next**: boundary & break {{< /button >}}