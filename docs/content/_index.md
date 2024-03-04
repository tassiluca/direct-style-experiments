# Direct style for Functional Reactive Programming: an analysis in Scala & Kotlin

## Goals of the project

> In the realm of asynchronous programming, the Scala ecosystem offers a set of solid monads constructs to tackle complex task functionally with elegance and efficiency, like [Monix Tasks](https://monix.io/docs/current/eval/task.html) and [Cats Effect](https://typelevel.org/cats-effect/).
>
> However, we are assisting to the increase in adoption of continuation and coroutines in modern runtimes, either exploiting some kind of fibers support, like the project Loom with Virtual Threads, or via code generation, like Kotlin Coroutines.
>
> The goal of this project is to delve into this field through the lens of direct style, developing few examples (not too complex) leveraging the new *strawman* library [Scala Gears](https://github.com/lampepfl/gears), comparing it with Kotlin's Coroutines and the current implementation of monadic Futures, seeking to analyze aspects such as:
>
> - ergonomics of the two styles (which one results more thoughtful and/or verbose);
> - which of the two approaches has a real advantage in adoption;
> - pros and cons of the two styles;
> - how and when to use one approach rather than the other;
> - any limitations and difficulties encountered in using them;

## Overview

The project is built around three main examples, delving from the fundamentals of the direct style frameworks for simple asynchronous computation to more complex reactive-like systems.

Here's the outline of the conducted analysis:

1. [`Boundary` and `break`](./docs/01-boundaries)
2. [Basic asynchronous constructs](./docs/02-basics)
3. [Channels as a communication primitive](./docs/03-channels)
4. [Reactivity in direct style](./docs/04-rears)
5. [Conclusions](./docs/05-going-further)

Code has been organized in Gradle submodules, one for each version of the examples (current monadic futures, Scala Gears, Kotlin Coroutines):


