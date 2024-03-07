# ...

## Context

In the realm of asynchronous programming, the Scala ecosystem offers a set of solid and widely adopted monadic constructs and libraries to tackle complex tasks functionally with elegance and efficiency, like Monix Tasks and Cats Effecs.
This monadic approach has enabled a wide range of nice features, like composable error handling, cancellation mechanisms and structured concurrency.

However, we are assisting to the increase in adoption of continuation and coroutines in modern runtimes, either exploiting some kind of fibers support, like the project Loom with Virtual Threads, or via code generation, like Kotlin Coroutines.

## Goals

The goal of this project is to delve into this field through the lens of direct style, developing a few examples (not too complex) leveraging the new strawman library Scala Gears, comparing it with Kotlin Coroutines and the current implementation of monadic Futures, seeking to analyze aspects such as:

- ergonomics of the two styles (which one result more thoughtful and/or verbose);
- which of the two approaches has a real advantage in adoption;
- pros and cons of the two styles;
- any limitations and difficulties encountered in using them.

## The contribution

The project is built around three main examples, focusing on different aspects.

The first is about a simple asynchronous computation that introduces the basics of asynchronous programming using direct style, focusing on the concept of structured concurrency and cancellation.

The second introduces the basic communication primitive of direct style, channels, and how they can be used to exchange data between concurrent tasks.

In this context, two contributions are proposed to extend the current Scala Gears library with:

- terminable channels, i.e. channels that can be terminated, but whose values can still be read by consumers;
- `Flow` Kotlin's Coroutines-like abstraction, modeling a *cold* stream of asynchronously computed values.

In the last example, it is investigated how to implement a reactive-like event-based system using direct style and, more specifically, the Scala Gears library.
To accomplish this, since the library lacks any kind of reactive transformation and operators, a small set of extension operators (inspired by Rx frameworks) are introduced on top of channels.

## Conclusions


## References

