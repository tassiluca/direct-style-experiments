# Blog posts service example: a direct-style vs monadic comparison

## The need for a new `Future` construct

The current implementation of the `Future` monadic construct suffers the following main cons:

- Lack of **referential transparency**;
- Lack of **cancellation** mechanisms and **structured concurrency**;
- **Accidental Sequentiality**.

To show these weaknesses in practice, a simple example of the core of a web service implementation is presented.

## Example: a blog posts service

{{< hint info >}}

**Idea**: develop a very simple (mocked) service which allows to retrieve and store from a repository blog posts, performing checks on the content and author before the actual storage.

{{</hint>}}

The example has been implemented using:

- the continuation style through the current Scala `Future` monadic constructs;
- the direct style, through:
  - the abstractions offered by _gears_;
  - _Kotlin coroutines_.

The example (and every subsequent one) is organized in three gradle submodules:

{{< mermaid class="optional" >}}
flowchart
  commons[blog-ws-commons]
  monadic[blog-ws-monadic]
  direct[blog-ws-direct]
  direct --> commons
  monadic --> commons
{{< /mermaid >}}

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

#### API

The API of the gears library is presented hereafter and is built on top of four main abstractions, three of them are here presented (the fourth in next example):

- **`Async`** context is **"a capability that allows a computation to suspend while waiting for the result of an async source"**. Code that has access to an instance of the `Async` trait is said to be in an async context and it is able to suspend its execution. Usually it is provided via `given` instances.
- **`Async.Source`** modeling an asynchronous source of data that can be polled or awaited by suspending the computation, as well as composed using combinator functions.
- **`Future`s** are the primary (in fact, the only) active elements that encapsulate a control flow that, eventually, will deliver a result (either a computed or a failure value that contains an exception). Since `Future`s are `Async.Source`s they can be awaited and combined with other `Future`s, suspending their execution.
  - **`Task`s** are the abstraction created to create delayed `Future`s, responding to the lack of referential transparency problem. They takes the body of a `Future` as an argument; its `run` method converts that body to a `Future`, starting its execution.

{{< mermaid >}}
classDiagram
  class Cancellable {
    &#60;&#60;trait&#62;&#62;
    +group: CompletionGroup
    +cancel()
    +link(group: CompletionGroup)
    +unlink()
  }

  class Tracking {
    &#60;&#60;trait&#62;&#62;
    +isCancelled Boolean
  }
  Cancellable <|-- Tracking

  class CompletionGroup {
    +add(member: Cancellable)
    +drop(member: Cancellable)
  }
  Tracking <|-- CompletionGroup 

  class Async {
    &#60;&#60;trait&#62;&#62;
    +group: CompletionGroup
    +await[T](src: Async.Source[T]) T
    +withGroup(group: CompletionGroup) Async
    +current() Async$
    +blocking[T](body: Async ?=> T) T$
    +group[T](body: Async ?=> T) T$
  }
  Async *--> CompletionGroup

  class `Async.Source[+T]` {
    &#60;&#60;trait&#62;&#62;
    +poll(k: Listener[T]) Boolean
    +poll() Option[T]
    +onComplete(k: Listener[T])
    +dropListener(k: Listener[T])
    +awaitResult() T
  }

  Async *--> `Async.Source[+T]`

  class `Listener[-T]` {
    &#60;&#60;trait&#62;&#62;
    +lock: Listener.ListenerLock | Null
    +complete(data: T, source: Async.Source[T])
    +completeNow(data: T, source: Async.Source[T]) Boolean
    +apply[T](consumer: (T, Source[T]) => Unit) Listener[T]$
  }

  `Async.Source[+T]` *--> `Listener[-T]`

  class OriginalSource {
    &#60;&#60;abstract class&#62;&#62;
  }
  `Async.Source[+T]` <|-- OriginalSource

  class `Future[+T]` {
    &#60;&#60;trait&#62;&#62;
    +apply[T](body: Async ?=> T) Future[T]$
    +now[T](result: Try[T]) Future[T]
    +zip[U](f2: Future[U]) Future[T, U]
    +alt(f2: Future[T]) Future[T]
    +altWithCancel(f2: Future[T]) Future[T]
  }
  class `Promise[+T]` {
    &#60;&#60;trait&#62;&#62;
    +asFuture Future[T]
    +complete(result: Try[T]) Unit
  }
  OriginalSource <|-- `Future[+T]`
  `Future[+T]` <|-- `Promise[+T]`

  class `Task[+T]` {
    +apply(body: (Async, AsyncOperations) ?=> T) Task[T]$
    +run: Future[+T]
  }
  `Task[+T]` *--> `Future[+T]`
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

As you can see, `Future`s are gone and the return type it's just the result of their intent (expressed with `Either` to return a meaningful message in case of failure). The fact they are _suspendable_ is expressed by means of the `Async` context which is required to invoke those function.

> Key inspiring principle (actually, "stolen" by Kotlin)
>
> ***&#10077;Concurrency is hard! Concurrency has to be explicit!&#10078;***

By default the code is serial. If you want to opt-in concurrency you have to explicitly use a `Future` or `Task` that spawn a new virtual thread, that executes it asynchronously allowing the caller to continue its execution.

The other important key feature of the proposed direct style is the support to **structured concurrency with cancellation**. This allow the nesting of future, meaning that the lifetime of nested computations is contained within the lifetime of enclosing ones, ensuring that...

The implementation of the `create` function with direct style in gears looks like this:

```scala
override def create(authorId: AuthorId, title: Title, body: Body)(using Async): Either[String, Post] =
  if context.repository.exists(title)
  then Left(s"A post entitled $title already exists")
  else either:
    val f = Future:
      val content = verifyContent(title, body).run // spawn of a new Future
      val author = authorBy(authorId).run // spawn of a new Future
      content.zip(author).await
    val (post, author) = f.awaitResult.?
    context.repository.save(Post(author, post.?._1, post.?._2, Date()))

/* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
private def authorBy(id: AuthorId): Task[Author] = ???

/* Some local computation that verifies the content of the post is appropriate (e.g. not offensive, ...). */
private def verifyContent(title: Title, body: Body): Task[Either[String, PostContent]] = ???
```

- description of APIs
  - Async `Source` and `Listeners`
    - combinators (`race` and `either`)
  - Futures, Promise
- serial by default
  - inspiring principle (taken by Kotlin Coroutines): "Concurrency is hard! Concurrency has to be explicit!"
- opt-in concurrency, using `Future`s
- referencial transparency using `Task`s
- structured + cancellation mechanisms
- use of boundaries: `?`
- suspension details on how it is implemented
- "Finally, about function coloring: Capabilities are actually much better here than other language's proposals such as suspend or async which feel clunky in comparison. This becomes obvious when you consider higher order functions. Capabilities let us define a single map (with no change in signature compared to now!) that works for sync as well as async function arguments. That's the real breakthrough here, which will make everything work so much smoother. I have talked about this elsewhere and this response is already very long, so I will leave it at that."

| **Combinator**  | **Goal**                                     |
|-----------------|----------------------------------------------|
| `zip`           | Parallel composition of two futures. If both futures succeed, succeed with their values in a pair. Otherwise, fail with the failure that was returned first |
| `alt`           | Alternative parallel composition of this task with other task. If either task succeeds, succeed with the success that was returned first. Otherwise, fail with the failure that was returned last. |
| `altWithCancel` | Like `alt` but the slower future is cancelled. |
| ...
