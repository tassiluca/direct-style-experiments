---
bookToc: false
---

# Basic asynchronous constructs

- [Basic asynchronous constructs](#basic-asynchronous-constructs)
  - [The need for a new `Future` construct](#the-need-for-a-new-future-construct)
  - [Example: a blog posts service](#example-a-blog-posts-service)
    - [Structure](#structure)
    - [Current monadic `Future`](#current-monadic-future)
    - [Direct style: Scala version with `gears`](#direct-style-scala-version-with-gears)
      - [Direct style vs. monadic style comparison w.r.t. composition](#direct-style-vs-monadic-style-comparison-wrt-composition)
    - [Kotlin Coroutines](#kotlin-coroutines)
  - [Takeaways](#takeaways)

## The need for a new `Future` construct

The current implementation of the `Future` monadic construct suffers the following main cons:

- Lack of **referential transparency**;
- Lack of **cancellation** mechanisms and **structured concurrency**;
- **Accidental Sequentiality**.

To show these weaknesses in practice, a simple example of the core of a web service implementation is presented.

## Example: a blog posts service

{{< hint info >}}

**Idea**: develop a very simple (mocked) service that allows retrieving and storing from a repository blog posts, performing checks on the content and author before the actual storage.

{{< /hint >}}

The example has been implemented using:

- the continuation style through the current Scala `Future` monadic constructs;
- the direct style, through:
  - the abstractions offered by _Gears_;
  - _Kotlin coroutines_.

The example is organized into Gradle submodules:

- `blog-ws-commons` contains code that has been reused for both the monadic and direct versions;
- `blog-ws-monadic` contains the monadic Scala style;
- `blog-ws-direct` contains the direct version using Scala Gears;
- `blog-ws-direct-kt` contains the direct version using Kotlin Coroutines.

For this example just the tests are provided. You can explore them in the `test` folders and run via Gradle using the name of the submodule:

```
./gradlew :blog-ws-<monadic | direct | direct-kt>:test
```

### Structure

The domain is modeled using abstract data types in a common `PostsModel` trait:

```scala
/** The model of a simple blog posts service. */
trait PostsModel:

  /** The post author's identifier. */
  type AuthorId

  /** The posts title. */
  type Title

  /** The posts body. */
  type Body

  /** The content of the post. */
  type PostContent = (Title, Body)

  /** A post author and their info. */
  case class Author(authorId: AuthorId, name: String, surname: String)

  /** A blog post, comprising an author, title, body and the last modification. */
  case class Post(author: Author, title: Title, body: Body, lastModification: Date)

  /** A function that verifies the content of the post, returning a [[scala.util.Success]] with
    * the content of  the post if the verification succeeds or a [[scala.util.Failure]] otherwise.
    */
  type ContentVerifier = (Title, Body) => Try[PostContent]

  /** A function that verifies the author has appropriate permissions, returning a
    * [[scala.util.Success]] with their information or a [[scala.util.Failure]] otherwise.
    */
  type AuthorsVerifier = AuthorId => Try[Author]
```

To implement the service two components have been conceived, following the Cake Pattern:

- `PostsRepositoryComponent`
  - exposes the `Repository` trait allowing to store and retrieve blog posts;
  - mocks a DB technology with an in-memory collection.
- `PostsServiceComponent`
  - is the component exposing the `Service` interface.
  - it could be called by the controller of the ReSTful web service.  

Both must be designed in an async way.

### Current monadic `Future`

The interface of the repository and services component of the monadic version are presented hereafter and their complete implementation is available [here](https://github.com/tassiLuca/direct-style-experiments/tree/master/blog-ws-monadic/src/main/scala/io/github/tassiLuca/dse/blog).

```scala
/** The component exposing blog posts repositories. */
trait PostsRepositoryComponent:
  context: PostsModel =>

  /** The repository instance. */
  val repository: PostsRepository

  /** The repository in charge of storing and retrieving blog posts. */
  trait PostsRepository:
    /** Save the given [[post]]. */
    def save(post: Post)(using ExecutionContext): Future[Post]

    /** @return a [[Future]] completed with true if a post exists with 
      *         the given title, false otherwise. */
    def exists(postTitle: Title)(using ExecutionContext): Future[Boolean]

    /** @return a [[Future]] completed either with a defined optional 
      *         post with given [[postTitle]] or an empty one. */
    def load(postTitle: Title)(using ExecutionContext): Future[Option[Post]]

    /** Load the post with the given [[postTitle]]. */
    def loadAll()(using ExecutionContext): Future[LazyList[Post]]
```

```scala
/** The component blog posts service. */
trait PostsServiceComponent:
  context: PostsRepositoryComponent & PostsModel =>

  /** The blog post service instance. */
  val service: PostsService

  /** The service exposing a set of functionalities to interact with blog posts. */
  trait PostsService:
    /** Creates a new blog post with the given [[title]] and [[body]], authored by [[authorId]]. */
    def create(authorId: AuthorId, title: Title, body: Body)(using ExecutionContext): Future[Post]

    /** Get a post from its [[title]]. */
    def get(title: Title)(using ExecutionContext): Future[Option[Post]]

    /** Gets all the stored blog posts in a lazy manner. */
    def all()(using ExecutionContext): Future[LazyList[Post]]
```

All the exposed functions, since they are asynchronous, return an instance of `Future[T]` and require to be called in a scope where a given instance of the `ExecutionContext` is declared.

What's important to delve into is the implementation of the service, and, more precisely, of the `create` method. As already mentioned, before saving the post two checks need to be performed:

1. the post author must have permission to publish a post and their information needs to be retrieved (supposing they are managed by another service);
2. the content of the post is analyzed in order to prevent the storage and publication of inappropriate content.

Since these operations are independent from each other they can be spawned and run in parallel.

```scala
override def create(authorId: AuthorId, title: Title, body: Body)(using ExecutionContext): Future[Post] =
  for
    exists <- context.repository.exists(title)
    if !exists
    post <- save(authorId, title, body)
  yield post

private def save(authorId: AuthorId, title: Title, body: Body)(using ExecutionContext): Future[Post] =
  val authorAsync = authorBy(authorId)
  val contentAsync = verifyContent(title, body)
  for
    content <- contentAsync
    author <- authorAsync
    post = Post(author, content._1, content._2, Date())
    _ <- context.repository.save(post)
  yield post

/* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
private def authorBy(id: AuthorId)(using ExecutionContext): Future[Author] = ???

/* Some local computation that verifies the content of the post is appropriate. */
private def verifyContent(title: Title, body: Body)(using ExecutionContext): Future[PostContent] = ???
```

This implementation shows the limits of the current monadic `Future` mechanism:

- if we want to achieve the serialization of futures execution we need to compose them using the `flatMap`, like in the `create` function: first, the check on the post existence is performed, and only if it is successful and another post with same title doesn't exist the `save` function is started
  - as a consequence, if we want two futures to run in parallel we have to spawn them before the `for-yield`, as in the `save` function. This is error-prone and could lead to unexpected sequentiality, like this:

    ```scala
    // THIS IS WRONG: the two futures are started sequentially!
    for
      content <- verifyContent(title, body)
      author <- authorBy(authorId)
      post = Post(author, content._1, content._2, Date())
      _ <- context.repository.save(post)
    yield post
    ```

- since the publication of a post can be performed only if both of these checks succeed, it is desirable that, whenever one of the two fails, the other gets canceled.
Unfortunately, currently, Scala Futures are not cancellable and provide no _structured concurrency_ mechanism.

- moreover, they lack referential transparency, i.e. future starts running when they are defined. This means that passing a reference to a future is not the same as passing the referenced expression.

### Direct style: Scala version with `gears`

The API of the gears library is presented hereafter and is built on top of four main abstractions, three of which are here presented (the fourth in the next example):

1. **`Async`** context is **"a capability that allows a computation to suspend while waiting for the result of an async source"**. Code that has access to an instance of the `Async` trait is said to be in an async context and can suspend its execution. Usually, it is provided via `given` instance:
   ```scala
    def suspendingFunction(using Async): Int 
   ```
   - `Async.blocking` creates an Async context *blocking the current thread for suspension* and it is good practice to use it only in the main function of an application (or in test suites).
      ```scala
      @main def launcher(): Unit = 
        Async.blocking:
          // inside this scope the `Async` capability is provided
          // (hence we can suspend and call suspendable functions!)
      ```
2. **`Async.Source`** model an asynchronous source of data that can be polled or awaited by suspending the computation, as well as composed using combinator functions.
3. **`Future`s** are the primary (in fact, the only) active elements that encapsulate a control flow that, eventually, will deliver a result (either a computed or a failure value that contains an exception). To **be spawned** an **`Async.Spawn` capability** is required, which is provided by the `Async.group` method.
    ```scala
    def suspendingFunction(using Async): Int = 
      Async.group: // needs the `Async` capability!
        // here the `Async.Spawn` capability is available, hence we can spawn Futures...
        Future:
          // ...
    ```
   - Since `Future`s are `Async.Source`s can be awaited and combined with other `Future`s, suspending their execution.
   - **`Task`s** are the abstraction created to create delayed `Future`s, responding to the lack of referential transparency problem. They take the body of a `Future` as an argument; its `run` method converts that body to a `Future`, starting its execution.
   - **`Promise`s** allow us to define `Future`'s result value externally, instead of executing a specific body of code.

{{< mermaid >}}
classDiagram
  class Async {
    << trait >>
    +group: CompletionGroup
    +withGroup(group: CompletionGroup) Async
    +await[T](src: Async.Source[T]) T
    +current() Async$
    +blocking[T](body: Async ?=> T) T$
    +group[T](body: Async ?=> T) T$
  }

  class `Async.Spawn` {
    << type >>
  }
  Async <|-- `Async.Spawn`

  class `Async.Source[+T]` {
    << trait >>
    +poll(k: Listener[T]) Boolean
    +poll() Option[T]
    +onComplete(k: Listener[T])
    +dropListener(k: Listener[T])
    +awaitResult()(using Async) T
  }

  Async *--> `Async.Source[+T]`

  class OriginalSource {
    << abstract class >>
  }
  `Async.Source[+T]` <|-- OriginalSource

  class `Listener[-T]` {
    << trait >>
    +lock: Listener.ListenerLock | Null
    +complete(data: T, source: Async.Source[T])
    +completeNow(data: T, source: Async.Source[T]) Boolean
    +apply[T](consumer: (T, Source[T]) => Unit) Listener[T]$
  }

  `Async.Source[+T]` *--> `Listener[-T]`

  class `Future[+T]` {
    << trait >>
    +apply[T](body: Async.Spawn ?=> T)(using Async, Async.Spawn) Future[T]$
    +now[T](result: Try[T]) Future[T]
    +zip[U](f2: Future[U]) Future[T, U]
    +or(f2: Future[T]) Future[T]
    +orWithCancel(f2: Future[T]) Future[T]
  }
  class `Promise[+T]` {
    << trait >>
    +apply() Promise[T]$
    +asFuture Future[T]
    +complete(result: Try[T])
  }
  OriginalSource <|-- `Future[+T]`
  `Future[+T]` <|-- `Promise[+T]`

  class `Task[+T]` {
    +apply(body: (Async, AsyncOperations) ?=> T) Task[T]$
    +start(using Async, Async.Spawn, AsyncOperations) Future[+T]
  }
  `Future[+T]` <--* `Task[+T]`

  class Cancellable {
    << trait >>
    +group: CompletionGroup
    +cancel()
    +link(group: CompletionGroup)
    +unlink()
  }

  Cancellable <|-- `Future[+T]`

  class Tracking {
    << trait >>
    +isCancelled Boolean
  }
  Cancellable <|-- Tracking

  class CompletionGroup {
    +add(member: Cancellable)
    +drop(member: Cancellable)
  }
  Tracking <|-- CompletionGroup

  Async *--> CompletionGroup

{{< /mermaid >}}

Going back to our example, the interface of both the repository and service components becomes ([here](https://github.com/tassiLuca/direct-style-experiments/tree/master/blog-ws-direct/src/main/scala/io/github/tassiLuca/dse/blog) you can find the complete sources):

```scala
/** The component exposing blog posts repositories. */
trait PostsRepositoryComponent:
  context: PostsModel =>

  /** The repository instance. */
  val repository: PostsRepository

  /** The repository in charge of storing and retrieving blog posts. */
  trait PostsRepository:
    /** Save the given [[post]]. */
    def save(post: Post)(using Async, CanFail): Post

    /** Return true if a post exists with the given title, false otherwise. */
    def exists(postTitle: Title)(using Async, CanFail): Boolean

    /** Load the post with the given [[postTitle]]. */
    def load(postTitle: Title)(using Async, CanFail): Option[Post]

    /** Load all the saved post. */
    def loadAll()(using Async, CanFail): LazyList[Post]
```

```scala
/** The blog posts service component. */
trait PostsServiceComponent:
  context: PostsRepositoryComponent & PostsModel =>

  /** The blog post service instance. */
  val service: PostsService

  /** The service exposing a set of functionalities to interact with blog posts. */
  trait PostsService:
    /** Creates a new blog post with the given [[title]] and [[body]], authored by [[authorId]],
      * or a string explaining the reason of the failure.
      */
    def create(authorId: AuthorId, title: Title, body: Body)(using Async, CanFail): Post

    /** Get a post from its [[title]] or a string explaining the reason of the failure. */
    def get(title: Title)(using Async, CanFail): Option[Post]

    /** Gets all the stored blog posts in a lazy manner or a string explaining the reason of the failure. */
    def all()(using Async, CanFail): LazyList[Post]
```

As you can see, **`Future`s are gone** and the return type it's just the result of their intent. 
The fact they are _suspendable_ is expressed using the `Async` context, which is required to invoke those functions. 
Since all these functions could fail (for example, because of a problem with the DB connection), the `CanFail` capability is used to model the effect of failure (as described in previous chapter).

> Key inspiring principle (actually, taken by Kotlin)
>
> ***&#10077;Concurrency is hard! Concurrency has to be explicit!&#10078;***

**By default the code is serial. If you want to opt-in concurrency you have to explicitly use a `Future` or `Task` spawning a new control flow that executes asynchronously, allowing the caller to continue its execution.**

The other important key feature of the library is the support for **structured concurrency and cancellation mechanisms**:

- `Future`s are `Cancellable` instances;
  - When you cancel a future using the `cancel()` method, it promptly sets its value to `Failure(CancellationException)`. Additionally, if it's a runnable future, the thread associated with it is interrupted using `Thread.interrupt()`.
  - to avoid immediate cancellation, deferring the cancellation after some block, is possible using `uninterruptible` function:

    ```scala
    val f = Future:
      // this can be interrupted
      uninterruptible:
        // this cannot be interrupted *immediately*
      // this can be interrupted
    ```

- `Future`s are nestable; **the lifetime of nested computations is contained within the lifetime of enclosing ones**. This is achieved using **`CompletionGroup`s**, which are cancellable objects themselves and serve as **containers for other cancellable objects**; **once they are canceled, all of their members are canceled as well**. Every `Async` context has a completion group tracking all computations in a tree structure, like the following:
  
  {{< figure src="../../res/img/completion-groups.svg" width="35%" alt="completion groups hierarchy" class="center" >}}

  **When a group terminates all its dangling children are canceled!**
  - The group is accessible through `Async.current.group`;
  - `Async.blocking`, `Async.group` and `Future` create a new completion group;
  - A cancellable object can be included inside the cancellation group of the async context using the `link` method; this is what the [implementation of the `Future` does, under the hood](https://github.com/lampepfl/gears/blob/07989ffdae153b2fe11ac1ece53ce9dd1dbd18ef/shared/src/main/scala/async/futures.scala#L140);
  - **to make sure children's computations are not canceled we need to await them.**

The implementation of the `create` function with direct style in Gears looks like this:

```scala
override def create(authorId: AuthorId, title: Title, body: Body)(using Async, CanFail): Post =
  if context.repository.exists(title) then fail(s"A post entitled $title already exists")
  val (post, author) = Async.group:
    val content = Future(verifyContent(title, body))
    val author = Future(authorBy(authorId))
    content.zip(author).awaitResult.?
  context.repository.save(Post(author, post._1, post._2, Date()))

/* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
private def authorBy(id: AuthorId)(using Async): Author = ...

/* Some local computation that verifies the content of the post is appropriate. */
private def verifyContent(title: Title, body: Body)(using Async): PostContent = ...
```

Some remarks:

- the `CanFail` capability is used to quickly break the computation with a meaningful message in case of failures;
- `authorBy` and `verifyContent` are suspendible functions, encapsulating the logic of the two checks;
- Thanks to structured concurrency and `zip` combinator we can achieve that if one of the nested two futures fails the other check is cancelled:
  - `zip`: combinator function returning a pair with the results if both `Future`s succeed, otherwise fail with the failure that was returned first;
  - in case of failure of one of the two futures, the `zip` returns immediately the control: `awaitResult` would return a `Failure(...)` and with `.?` we break prematurely the computation, leaving the `Async.group`, thus canceling all dangling `Future`!
    - `authorBy` and `verifyContent` needs to be programmed to throw an exception in case of failure. This is needed to make `Future` fail: without fail with an exception, the `zip` operator is not able to return immediately the control and the other future is not canceled!
  - Be aware of the fact to achieve cancellation is necessary to enclose both the content verification and authorization task inside a completion group (either using `Async.group` or `Future`), since the `zip` doesn't provide a cancellation mechanism per se. The following code wouldn't work as expected!
    ```scala
    // WRONG: doesn't provide cancellation!
    val contentVerifier = verifyContent(title, body).run
    val authorizer = authorBy(authorId).run
    val (post, author) = contentVerifier.zip(authorizer).awaitResult.?
    ```

👉🏻 To showcase the structured concurrency and cancellation mechanisms of Scala Gears tests have been prepared:

- [`StructuredConcurrencyTest`](https://github.com/tassiLuca/direct-style-experiments/blob/master/commons/src/test/scala/io/github/tassiLuca/dse/StructuredConcurrencyTest.scala)
- [`CancellationTest`](https://github.com/tassiLuca/direct-style-experiments/blob/master/commons/src/test/scala/io/github/tassiLuca/dse/CancellationTest.scala)

Other combinator methods, available on `Future`s instance:

| **Combinator**                       | **Goal**                                      |
|--------------------------------------|---------------------------------------------- |
| `Future[T].zip(Future[U])`           | Parallel composition of two futures. If both futures succeed, succeed with their values in a pair. Otherwise, fail with the failure that was returned first |
| `Future[T].or(Future[T])` / `Seq[Future[T]].awaitFirst` | Alternative parallel composition. If either task succeeds, succeed with the success that was returned first. Otherwise, fail with the failure that was returned last (race all futures). |
| `Future[T].orWithCancel(Future[T])` / `Seq[Future[T]].awaitFirstWithCancel` | Like `or`/`awaitFirst` but the slower futures are cancelled. |
| `Seq[Future[T]].awaitAll`            | `.await` for all futures in the sequence, returns the results in a sequence, or throws if any futures fail. |
| `Seq[Future[T]].awaitAllOrCancel`    | Like `awaitAll`, but cancels all futures as soon as one of them fails. |

#### Direct style vs. monadic style comparison w.r.t. composition

Direct style cleanly supports composability:

```scala
def transform[E, T](xs: Seq[Future[Either[E, T]]])(using Async.Spawn): Future[Either[E, Seq[T]]] =
  Future:
    either:
      xs.map(_.await.?)
```

Using monads is more complex to achieve the same goal:

```scala
def transform[E, T](
    xs: Seq[Future[Either[E, T]]],
)(using ExecutionContext): Future[Either[E, Seq[T]]] =
  val initial: Future[Either[E, List[T]]] = Future.successful(Right(List.empty[T]))
  xs.foldRight(initial): (future, acc) =>
    for
      f <- future
      a <- acc
    yield a.flatMap(lst => f.map(_ :: lst))
```

Again, using Cats simplifies the code, still, it's more complex than the direct style:

```scala
def transform[E, T](
    xs: Seq[Future[Either[E, T]]],
)(using ExecutionContext): Future[Either[E, Seq[T]]] =
  import cats.implicits._
  Future.sequence(xs) // Future[Seq[Either[E, T]]
    .map(_.sequence) // equivalent to: _.traverse(identity)
```

[[Ref]](https://github.com/tassiLuca/direct-style-experiments/blob/master/commons/src/main/scala/io/github/tassiLuca/dse/examples/gears/ShowcasingDirectStyle.scala)

### Kotlin Coroutines

- A **coroutine** in Kotlin is an instance of a *suspendable* computation.
- Their API is quite similar to the Scala Gears, which has taken inspiration from Kotlin coroutines. To try to make a comparison, the following table shows the correspondence between the two libraries:
  | **Scala Gears**            | **Kotlin Coroutines**           |
  |----------------------------|---------------------------------|
  | `Async`                    | `CoroutineScope`                |
  | `Future[Unit]`             | `Job`                           |
  | `Future[T]`                | `Deferred<T>`                   |
  | `def all()(using Async)`   | `suspend fun all()`             |

    - In the Kotlin Coroutine library, `CoroutineScope` is an interface that defines a single property, `coroutineContext`, which returns the `CoroutineContext` that defines the scope in which the coroutine runs.

      ```kotlin
        public interface CoroutineScope {
            /** Returns the context of this scope. */
            public val coroutineContext: CoroutineContext
        }
        ```

        Every coroutine must be executed in a coroutine context, which is a collection of key-value pairs that provide contextual information for the coroutine, including a dispatcher, that determines what thread or threads the coroutine uses for its execution, and the `Job` of the coroutine, which represents a cancellable background piece of work with a life cycle that culminates in its completion.

      - Different ways to create a scope:
        - `GlobalScope.launch` launching a new coroutine in the global scope -- *discouraged because it can lead to memory leaks*;
        - `CoroutineScope(Dispatchers.Default)`, using the constructor with a dispatcher;
        - `runBlocking` - equivalent to the Gears `Async.blocking` - provides a way to run a coroutine in the `MainScope`, i.e. on the main/UI thread.
  
      - Useful dispatchers:
        - `Default dispatcher`: to run CPU-intensive functions. If we forget to choose our dispatcher, this dispatcher will be selected by default;
        - `IO dispatcher`: to run I-O bound computation, where we block waiting for input-output operations to complete, like network-related operations, file operations, etc.;
        - `Unconfined dispatcher`: it isn't restricted to a particular thread, i.e. doesn't change the thread of the coroutine, it operates on the same thread where it was initiated;
        - `Main dispatcher`: used when we want to interact with the UI. It is restricted to the main thread.

      - Several coroutine builders exist, like `launch`, `async`, `withContext` which accept an optional `CoroutineContext` parameter that can be used to specify the dispatcher and other context elements.

      ```kotlin
      fun main(): Unit = runBlocking { // this: CoroutineScope
          val job: Job = this.launch(Dispatchers.IO) { // launch a new coroutine and continue
              delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
              print("Kotlin Coroutine!") // print after delay
          }
          val job2: Deferred<String> = async {
              delay(100L)
              "Hello"
          }
          print(job2.await() + " ") // wait until child coroutine completes
          job.join() // wait until the job is done and "Kotlin Coroutine!" is printed
      }
      ```

    - suspending functions are marked with the `suspend` keyword; they can use other suspending functions to suspend the execution of a coroutine.

- Coroutines follow the principle of structured concurrency: coroutines can be arranged into parent-child hierarchies where the cancellation of a parent leads to the immediate cancellation of all its children recursively. Failure of a child with an exception immediately cancels its parent and, consequently, all its other children.

Going back to our example, the interface of the service with Kotlin coroutines looks like this ([here](https://github.com/tassiLuca/direct-style-experiments/tree/master/blog-ws-direct-kt/src/main/kotlin/io/github/tassiLuca/dse/blog) you can find the complete sources):

```kotlin
/** The service exposing a set of functionalities to interact with blog posts. */
interface PostsService {

    /** Creates a new post. */
    suspend fun create(authorId: String, title: String, body: String): Result<Post>

    /** Retrieves a post by its title. */
    suspend fun get(title: String): Result<Post>

    /** Retrieves all the posts. */
    suspend fun getAll(): Result<Sequence<Post>>
}
```

The implementation of the `create` function:

```kotlin
override suspend fun create(authorId: String, title: String, body: String): Result<Post> = runCatching {
    coroutineScope {
        require(!repository.exists(title)) { "Post with title $title already exists" }
        val content = async { verifyContent(title, body) }
        val author = async { authorBy(authorId) }
        val post = Post(author.await(), content.await(), Date())
        repository.save(post)
    }
}

/* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
private suspend fun authorBy(id: String): Author { ... }

/* Some local computation that verifies the content of the post is appropriate. */
private suspend fun verifyContent(title: String, body: String): PostContent { ... }
```

- a `coroutineScope` is a suspending function used to create a new coroutine scope: it suspends the execution of the current coroutine, releasing the underlying thread for other usages;
- As we said previously, the failure of a child with an exception immediately cancels its parent and, consequently, all its other children: this means that, for handling the cancellation of nested coroutines, we don't need to do anything special
  - with `coroutineScope` no matter the order in which coroutines are awaited, if one of them fails with an exception it is propagated upwards, cancelling all other ones
    - this is not the case for `supervisorScope`, a coroutine builder ensuring that child coroutines can fail independently without affecting the parent coroutine.
    - have a look to [this test](https://github.com/tassiLuca/direct-style-experiments/blob/master/blog-ws-direct-kt/src/test/kotlin/io/github/tassiLuca/dse/CoroutinesCancellationTests.kt)
  - This is an advantage over the Scala Gears, where operators like `zip` and `altWithCancel` are necessary!

## Takeaways

> - Scala Gears offers, despite the syntactical differences, very similar concepts to Kotlin Coroutines, with structured concurrency and cancellation mechanisms;
> - Kotlin Coroutines handles the cancellation of nested coroutines more easily than Scala Gears, where special attention is required;
> - As [stated by M. Odersky](https://github.com/lampepfl/gears/issues/19#issuecomment-1732586362) the `Async` capability is better than `suspend` because let defines functions that work for synchronous as well as asynchronous function arguments without changing anything, while in Kotlin suspendable functions passed as an argument in higher-order functions must be tagged with `suspend` keyword.

{{< button relref="/02-boundaries" >}} **Previous**: boundary & break{{< /button >}}

{{< button relref="/04-channels" >}} **Next**: Channels as a communication primitive {{< /button >}}
