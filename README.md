# Direct style for Functional Reactive Programming: an analysis in Scala & Kotlin

## Goals of the project

In the realm of asynchronous programming, the Scala ecosystem offers a set of solid monads constructs and libraries to tackle complex task functionally with elegance and efficiency, like [Monix Tasks](https://monix.io/docs/current/eval/task.html) and [Cats Effect](https://typelevel.org/cats-effect/).

However, we are assisting to the increase in adoption of continuation and coroutines in modern runtimes, either exploiting some kind of fibers support, like the project Loom with Virtual Threads, or via code generation, like Kotlin Coroutines.

The goal of this project is to delve into this field through the lens of direct style, developing few examples (not too complex) leveraging the new *strawman* library [Scala Gears](https://github.com/lampepfl/gears), comparing it with Kotlin's Coroutines and the current implementation of monadic Futures, seeking to analyze aspects such as:

- ergonomics of the two styles (which one results more thoughtful and/or verbose);
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
│   │        ├─ examples     # some common examples
│   │        └─ pimping      # proposed extensions to the Scala Gears library
│   └─ test                  # general tests (cancellation, structured concurrency, ...)
├── rears                    # extensions to the Scala Gears library for Rx
├── smart-hub-direct         # smart hub example using Scala Gears
└── smart-hub-direct-kt      # smart hub example using Kotlin Coroutines
```

**IMPORTANT NOTE: Examples works with a version of the JDK > 21** (Virtual Threads are needed!).

To build and run all the tests:

```
./gradlew build
```

Generally speaking, the runnable examples can be run by simply executing the `run` Gradle task in the respective submodule, like:

```
./gradlew :analyzer-direct:run
```

Detailed instructions can be found in the `README` file of each submodule and in the [documentation](https://tassiluca.github.io/direct-style-experiments/).
