# Analysis of direct style (for asynchronous reactive programming) in Scala

## Context

In the realm of asynchronous programming, the Scala ecosystem offers a set of solid and widely adopted monadic constructs and libraries to tackle complex tasks functionally with elegance and efficiency, like [Monix Tasks](https://monix.io/docs/current/eval/task.html) and [Cats Effecs](https://typelevel.org/cats-effect/), enabling a wide range of interesting and useful features, like composable error handling, cancellation mechanisms and structured concurrency that the standard library lacks.
However, they also come with a cost: the pervasiveness of the `flatMap` operator to compose values makes the code harder to reason about and difficult and awkward to integrate with regular control structures.

In the last years, we have been assisting the increase in adoption of continuations and coroutines in modern runtimes, either exploiting some kind of fiber support, like the project Loom with Virtual Threads, or via code generation, like [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html).
Even Scala is not immune to this trend and a new strawman library, [Gears](https://github.com/lampepfl/gears), is currently being developed, aiming to bring direct style support for asynchronous programming.
Despite the interest and the potential this new library could bring, it is just a speck that fits into a bigger picture, which is the management of effects: the ongoing research activity led by M. Odersky has indeed the goal to, instead of pushing effect management into external libraries, upgrade the type system to handle effects natively using capabilities, as research-oriented programming languages do with Algebraic Effects (like Koka).

## Goals

The goal of this project is to explore, ***mainly focusing on Scala***, the direct style, developing a few examples (not too complex) leveraging the new strawman library Gears for asynchronous programming, comparing it with Kotlin Coroutines and the current implementation of monadic Futures, seeking to analyze aspects such as:

- ergonomics of the two styles;
- which of the two approaches has a real advantage in adoption;
- pros and cons of the two styles;
- any limitations and difficulties encountered in using them.

## Overview

The project is built around three main examples, delving from the fundamentals of the direct style frameworks for simple asynchronous computation to more complex reactive-like systems.
The full discussion of these examples can be found in the [documentation](https://tassiluca.github.io/direct-style-experiments/).

Code has been organized in Gradle submodules, one for each version of the examples (current monadic futures, Scala Gears, Kotlin Coroutines).
Here an overview of the project folder structure:

```plaintext
direct-style-experiments
├── analyzer-commons         # common code for analyzers (UI, controller, ...)
├── analyzer-direct          # analyzer example using Scala Gears
├── analyzer-direct-kt       # analyzer example using Kotlin Coroutines
├── analyzer-monadic         # analyzer example using current Futures
├── blog-ws-commons          # common code for the blog service example
├── blog-ws-direct           # blog service example using Scala Gears
├── blog-ws-direct-kt        # blog service example using Kotlin Coroutines
├── blog-ws-monadic          # blog service example using current Futures
├── commons/                 # modules with common code for entire project
│   ├─ src/
│   │  └─ main/
│   │     └─ scala/
│   │        ├─ boundaries   # `boundary` and `break` implementations
│   │        ├─ examples     # some examples
│   │        └─ pimping      # proposed extensions to the Scala Gears library
│   └─ test                  # general tests (cancellation, structured concurrency, ...)
├── rears                    # extensions to the Scala Gears library for Rx
├── smart-hub-direct         # smart hub example using Scala Gears
└── smart-hub-direct-kt      # smart hub example using Kotlin Coroutines
```

**:warning: Examples works with a version of the JDK > 21** (Virtual Threads are needed!).

To build and run all the tests:

```
./gradlew build
```

Generally speaking, the runnable examples can be run by simply executing the `run` Gradle task in the respective submodule, like:

```
./gradlew :analyzer-direct:run
```

Detailed instructions can be found in the `README` file of each submodule and in the [documentation](https://tassiluca.github.io/direct-style-experiments/).
