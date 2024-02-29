---
bookToc: false
---

# Basic asynchronous constructs

## The need for a new `Future` construct

The current implementation of the `Future` monadic construct suffers the following main cons:

- Lack of **referential transparency**;
- Lack of **cancellation** mechanisms and **structured concurrency**;
- **Accidental Sequentiality**.

To show these weaknesses in practice, a simple example of the core of a web service implementation is presented.

## Example: a blog posts service

{{< hint info >}}

**Idea**: develop a very simple (mocked) service which allows to retrieve and store from a repository blog posts, performing checks on the content and author before the actual storage.

{{< /hint >}}

The example has been implemented using:

- the continuation style through the current Scala `Future` monadic constructs;
- the direct style, through:
  - the abstractions offered by _gears_;
  - _Kotlin coroutines_.

The example (and every subsequent one) is organized in three gradle submodules:

- `blog-ws-commons` contains code which has been reused for both the monadic and direct versions;
- a submodule `blog-ws-monadic` with the monadic Scala style and `blog-ws-direct` for the direct versions, both in Kotlin with _coroutines_ and in Scala with _gears_.

### Structure

The domain is modelled using abstract data types in a common `PostsModel` trait:

```scala
trait PostsModel:
  type AuthorId
  type Title
  type Body
  type PostContent = (Title, Body)

  /** A blog post, comprising of an author, title, body and the information about last modification. */
  case class Post(author: Author, title: Title, body: Body, lastModification: Date)

  /** A post author and their info. */
  case class Author(authorId: AuthorId, name: String, surname: String)

  /** A function that verifies the content of the post, 
    * returning [[Right]] with the content of the post if the
    * verification succeeds or [[Left]] with the reason why failed.
    */
  type ContentVerifier = (Title, Body) => Either[String, PostContent]

  type AuthorsVerifier = AuthorId => Author
```

To implement the service two components have been conceived, following the Cake Pattern:

- `PostsRepositoryComponent`
  - exposes the `Repository` trait allowing to store and retrieve blog posts;
  - mocks a DB technology with an in-memory collection.
- `PostsServiceComponent`
  - is the component exposing the `Service` interface.
  - is the component that would be called by the controller of the ReSTful web service.  

Both must be designed in an async way.

### Current monadic `Future`

The interface of the repository and services component of the monadic version are presented hereafter and their complete implementation is available [here]().

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

    /** Return a future completed with true if a post exists with the given title, false otherwise. */
    def exists(postTitle: Title)(using ExecutionContext): Future[Boolean]

    /** Load all the saved post. */
    def load(postTitle: Title)(using ExecutionContext): Future[Option[Post]]

    /** Load the post with the given [[postTitle]]. */
    def loadAll()(using ExecutionContext): Future[LazyList[Post]]
```

```scala
/** The component blog posts service. */
trait PostsServiceComponent:
  context: PostsRepositoryComponent with PostsModel =>

  /** The blog post service instance. */
  val service: PostsService

  /** The service exposing a set of functionalities to interact with blog posts. */
  trait PostsService:
    /** Creates a new blog post with the given [[title]] and [[body]], authored by [[authorId]]. */
    def create(authorId: AuthorId, title: Title, body: Body)(using ExecutionContext): Future[Post]

    /** Get a post from its [[title]]. */
    def get(title: Title)(using ExecutionContext): Future[Post]

    /** Gets all the stored blog posts in a lazy manner. */
    def all()(using ExecutionContext): Future[LazyList[Post]]
```

All the exposed functions, since they are asynchronous, returns an instance of `Future[T]` and requires to be called in a scope where a given instance of the `ExecutionContext` is declared.

What's important to delve into is the implementation of the service, and, more precisely, of the `create` method. As already mentioned, before saving the post two checks needs to be performed:

1. the post author must have permissions to publish a post and their information needs to be retrieved (supposing they are managed by another microservice);
2. the content of the post is analyzed in order to prevent the storage and publication of offensive or non-appropriate contents.

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
```

This implementation shows the limits of the current monadic `Future` mechanism:

- if we want to achieve the serialization of future's  execution we need to compose them using the `flatMap`, like in the `create` function: first the check on the post existence is performed, and only if it successful and another post with same title doesn't exists the `save` function is started
  - as a consequence, if we want two futures to run in parallel we have to spawn them before the `for-yield`, as in the `save` function, or use Future's Applicative, like `mapN` provided by Cats. This is error prone and could lead to unexpected sequentiality for non experted Scala programmers, like this:

    ```scala
      for
        content <- verifyContent(title, body)
        author <- authorBy(authorId)
        post = Post(author, content._1, content._2, Date())
        _ <- context.repository.save(post)
      yield post
    ```

- since the publication of a post can be performed only if both of these checks succeeds, it is desirable that, whenever one of the two fails, the other get cancelled.
Unfortunately, currently, Scala Futures are not cancellable and provides no _structured concurrency_ mechanism.

- moreover, they lack referential transparency, i.e. future starts running when they are defined. This mean that passing a reference to a future is not the same as passing the referenced expression.

### Direct style: Scala version with `gears`

The API of the gears library is presented hereafter and is built on top of four main abstractions, three of them are here presented (the fourth in next example):

1. **`Async`** context is **"a capability that allows a computation to suspend while waiting for the result of an async source"**. Code that has access to an instance of the `Async` trait is said to be in an async context and it is able to suspend its execution. Usually it is provided via `given` instances.
   - A common way to obtain an `Async` instance is to use an `Async.blocking`.
2. **`Async.Source`** modeling an asynchronous source of data that can be polled or awaited by suspending the computation, as well as composed using combinator functions.
3. **`Future`s** are the primary (in fact, the only) active elements that encapsulate a control flow that, eventually, will deliver a result (either a computed or a failure value that contains an exception). Since `Future`s are `Async.Source`s they can be awaited and combined with other `Future`s, suspending their execution.
   - **`Task`s** are the abstraction created to create delayed `Future`s, responding to the lack of referential transparency problem. They takes the body of a `Future` as an argument; its `run` method converts that body to a `Future`, starting its execution.
   - **`Promise`s** allows to define `Future`'s result value externally, instead of executing a specific body of code.

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

  class `Async.Source[+T]` {
    << trait >>
    +poll(k: Listener[T]) Boolean
    +poll() Option[T]
    +onComplete(k: Listener[T])
    +dropListener(k: Listener[T])
    +awaitResult() T
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
    +apply[T](body: Async ?=> T) Future[T]$
    +now[T](result: Try[T]) Future[T]
    +zip[U](f2: Future[U]) Future[T, U]
    +alt(f2: Future[T]) Future[T]
    +altWithCancel(f2: Future[T]) Future[T]
  }
  class `Promise[+T]` {
    << trait >>
    +asFuture Future[T]
    +complete(result: Try[T])
  }
  OriginalSource <|-- `Future[+T]`
  `Future[+T]` <|-- `Promise[+T]`

  class `Task[+T]` {
    +apply(body: (Async, AsyncOperations) ?=> T) Task[T]$
    +run(using Async, AsyncOperations) Future[+T]
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

Going back to our example, the interface of both the repository and service components becomes:

```scala
/** The component exposing blog posts repositories. */
trait PostsRepositoryComponent:
  context: PostsModel =>

  /** The repository instance. */
  val repository: PostsRepository

  /** The repository in charge of storing and retrieving blog posts. */
  trait PostsRepository:
    /** Save the given [[post]]. */
    def save(post: Post)(using Async): Post

    /** Return true if a post exists with the given title, false otherwise. */
    def exists(postTitle: Title)(using Async): Boolean

    /** Load the post with the given [[postTitle]]. */
    def load(postTitle: Title)(using Async): Option[Post]

    /** Load all the saved post. */
    def loadAll()(using Async): LazyList[Post]
```

```scala
/** The blog posts service component. */
trait PostsServiceComponent:
  context: PostsRepositoryComponent with PostsModel =>

  /** The blog post service instance. */
  val service: PostsService

  /** The service exposing a set of functionalities to interact with blog posts. */
  trait PostsService:
    /** Creates a new blog post with the given [[title]] and [[body]], authored by [[authorId]], or a string explaining
      * the reason of the failure.
      */
    def create(authorId: AuthorId, title: Title, body: Body)(using Async): Either[String, Post]

    /** Get a post from its [[title]] or a string explaining the reason of the failure. */
    def get(title: Title)(using Async): Either[String, Post]

    /** Gets all the stored blog posts in a lazy manner or a string explaining the reason of the failure. */
    def all()(using Async): Either[String, LazyList[Post]]
```

As you can see, `Future`s are gone and the return type it's just the result of their intent (expressed with `Either` to return a meaningful message in case of failure). The fact they are _suspendable_ is expressed by means of the `Async` context, which is required to invoke those function.

> Key inspiring principle (actually, "stolen" by Kotlin)
>
> ***&#10077;Concurrency is hard! Concurrency has to be explicit!&#10078;***

By default the code is serial. If you want to opt-in concurrency you have to explicitly use a `Future` or `Task` spawning a new control flow that executes asynchronously, allowing the caller to continue its execution.

The other important key feature of the library is the support to **structured concurrency and cancellation mechanisms**:

- `Future`s are `Cancellable` instances;
  - When you cancel a future using the `cancel()` method, it promptly sets its value to `Failure(CancellationException)`. Additionally, if it's a runnable future, the thread associated with it is interrupted using `Thread.interrupt()`.
  - to avoid the immediate cancellation, deferring the cancellation after some block is possible using `uninterruptible` function:

    ```scala
    val f = Future {
      // this can be interrupted
      uninterruptible {
        // this cannot be interrupted *immediately*
      }
      // this can be interrupted
    }
    ```

- `Future`s are nestable; it is assured that the lifetime of nested computations is contained within the lifetime of enclosing ones. This is achieved using `CompletionGroup`s, which are cancellable objects themselves and serves as containers for other cancellable objects, that once cancelled, all of its members are cancelled as well.
  - A cancellable object can be included inside the cancellation group of the async context using the `link` method; this is what the [implementation of the `Future` does, under the hood](https://github.com/lampepfl/gears/blob/07989ffdae153b2fe11ac1ece53ce9dd1dbd18ef/shared/src/main/scala/async/futures.scala#L140).

The implementation of the `create` function with direct style in gears looks like this:

```scala
override def create(authorId: AuthorId, title: Title, body: Body)(using Async): Either[String, Post] =
  if context.repository.exists(title)
  then Left(s"A post entitled $title already exists")
  else either:
    val f = Future:
      val content = verifyContent(title, body).run // spawning a new Future
      val author = authorBy(authorId).run // spawninig a new Future
      content.zip(author).await
    val (post, author) = f.awaitResult.?
    context.repository.save(Post(author, post.?._1, post.?._2, Date()))

/* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
private def authorBy(id: AuthorId): Task[Author] = ???

/* Some local computation that verifies the content of the post is appropriate (e.g. not offensive, ...). */
private def verifyContent(title: Title, body: Body): Task[Either[String, PostContent]] = ???
```

Some remarks:

- the `either` boundary have been used to quickly return a `Right[String, Post]` object in case something goes wrong;
- `authorBy` and `verifyContent` returns referential transparent `Task` instances. Running them, spawns a new `Future` instance;
- Thanks to structured concurrency and `zip` combinator we can obtain that if one of the nested two futures fails the enclosing future is cancelled, cancelling also all its unterminated children
  - `zip`: combinator function returning a pair with the results if both `Future`s succeed, otherwise fail with the failure that was returned first.
  - Be aware of the fact to achieve cancellation is necessary to enclose both the content verification and authorization task inside an enclosing `Future`, since the `zip` doesn't provide cancellation mechanism per se. The following code wouldn't work as expected!
    ```scala
    val contentVerifier = verifyContent(title, body).run
    val authorizer = authorBy(authorId).run
    val (post, author) = contentVerifier.zip(authorizer).awaitResult.?
    ```

Other combinator methods, available on `Future`s instance:

| **Combinator**                       | **Goal**                                      |
|--------------------------------------|---------------------------------------------- |
| `Future[T].zip(Future[U])`           | Parallel composition of two futures. If both futures succeed, succeed with their values in a pair. Otherwise, fail with the failure that was returned first |
| `Future[T].alt(Future[T])` / `Seq[Future[T]].altAll` | Alternative parallel composition. If either task succeeds, succeed with the success that was returned first. Otherwise, fail with the failure that was returned last (race all futures). |
| `Future[T].altWithCancel(Future[T])` / `Seq[Future[T]].altAllWithCancel` | Like `alt` but the slower future is cancelled. |
| `Seq[Future[T]].awaitAll`            | `.await` for all futures in the sequence, returns the results in a sequence, or throws if any futures fail. |
| `Seq[Future[T]].awaitAllOrCancel`    | Like `awaitAll`, but cancels all futures as soon as one of them fails. |

---

TO FINISH

tests

kotlin coroutines

w.r.t. kotlin coroutines:

- "Finally, about function coloring: Capabilities are actually much better here than other language's proposals such as suspend or async which feel clunky in comparison. This becomes obvious when you consider higher order functions. Capabilities let us define a single map (with no change in signature compared to now!) that works for sync as well as async function arguments. That's the real breakthrough here, which will make everything work so much smoother. I have talked about this elsewhere and this response is already very long, so I will leave it at that."

how suspension is implemented

---

## Conclusions
