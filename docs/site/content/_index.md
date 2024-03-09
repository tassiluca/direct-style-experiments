# Direct style for Functional Reactive Programming: an analysis in Scala & Kotlin

The project is built around three main examples, delving from the fundamentals of the direct style frameworks for simple asynchronous computation to more complex reactive-like systems.

Here's the outline of the conducted analysis:

1. [Overview of the project: goals, contribution, and conclusions](./docs/01-overview)
2. [`Boundary` and `break`](./docs/02-boundaries)
3. [Basic asynchronous constructs](./docs/03-basics)
4. [Channels as a communication primitive](./docs/04-channels)
5. [Reactivity in direct style](./docs/05-rears)

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
