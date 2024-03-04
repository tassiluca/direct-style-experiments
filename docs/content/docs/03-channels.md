---
bookToc: false
---

# Channels as a communication primitive

- [Channels as a communication primitive](#channels-as-a-communication-primitive)
  - [Introduction](#introduction)
  - [Analyzer example](#analyzer-example)
    - [Future monadic version](#future-monadic-version)
    - [Scala Gears version](#scala-gears-version)
    - [Kotlin Coroutines version](#kotlin-coroutines-version)
  - [Introducing `Flow`s in Gears](#introducing-flows-in-gears)
    - [Showcasing `Flow`s](#showcasing-flows)
  - [Takeaways](#takeaways)

## Introduction

The fourth, yet not mentioned, abstraction of both Kotlin Coroutines and Scala Gears is the **channel**.
Channels represent the **primitive communication and coordination means** to exchange `Future` (or coroutines in the case of Kotlin) results. They are, at least conceptually, very similar to a queue where it is possible to send (and receive) data -- basically, exploiting the ***producer-consumer*** pattern.

{{< figure src="../../res/img/channel.svg" alt="Channel" class="center" >}}

{{< mermaid >}}
classDiagram
  class `SendableChannel[-T]` {
    << trait >>
    +sendSource(x: T) Async.Source[Either[Closed, Unit]]
    +send(x: T)(using Async) Unit
  }

  class `ReadableChannel[+T]` {
    << trait >>
    +readSource Async.Source[Either[Closed, T]]
    +read()(using Async) Either[Closed, T]
  }

  class `Channel[T]` {
    << trait >>
    +asSendable: SendableChannel[T]
    +asReadable: ReadableChannel[T]
    +asCloseable: java.io.Closeable
  }

  namespace java io {
    class Closeable {
      << interface >>
      +close()
    }
  }

  `SendableChannel[-T]` <|-- `Channel[T]`
  Closeable <|-- `Channel[T]`
  `ReadableChannel[+T]` <|-- `Channel[T]`
{{< /mermaid >}}

The channel is defined through three distinct interfaces: `SendableChannel[-T]`, `ReadableChannel[+T]` and `Channel[T]`, where the latter extends from both `SendableChannel` and `ReadableChannel`. Typically, a `Channel` is created and a `SendableChannel` and `ReadableChannel` instances are respectively provided to the producer and the consumer, restricting their access to it. The same, almost identical, design is present also in Kotlin Coroutines where `SendChannel` and `ReceiveChannel` take over, respectively, the Gears `SendableChannel` and `ReadableChannel`.

{{< hint warning >}}

`Channel` inherits from `java.io.Closable`, making them closable objects: once closed, they raise `ChannelClosedException` when attempting to write to them and immediately return a `Left(Closed)` when attempting to read from them, preventing the consumer from finishing reading all the values sent on the channel before its closing.
This is not the case for Kotlin Coroutines where closing a channel indicates that no more values are coming, but doesn't prevent consuming already sent values. Moreover, in Kotlin is possible to use a regular for loop to receive elements from a channel (blocking the coroutine):

{{< /hint >}}

```kotlin
val channel = Channel<Int>()
launch {
    for (x in 1..5) channel.send(x * x)
    channel.close() // we're done sending
}
for (y in channel) println(y) // blocks until channel is closed
println("Done!")
```

{{< hint info >}}

Similar behavior can be achieved also in Gears extending the framework with the concept of **`Terminable`** channel. After all, closing a channel in coroutines is a matter of sending a special token to it, allowing stop the iteration as soon as this token is received.

{{< /hint >}}

[[The full implementation can be found in `commons` submodule](https://github.com/tassiLuca/PPS-22-direct-style-experiments/blob/master/commons/src/main/scala/io/github/tassiLuca/pimping/TerminableChannel.scala).]

```scala
/** A token to be sent to a channel to signal that it has been terminated. */
case object Terminated

type Terminated = Terminated.type

/** A union type of [[T]] and [[Terminated]]. */
type Terminable[T] = T | Terminated

/** Exception being raised by [[TerminableChannel.send()]] on terminated [[TerminableChannel]]. */
class ChannelTerminatedException extends Exception

/** A [[Channel]] that can be terminated, signalling no more items will be sent,
  * still allowing to consumer to read pending values.
  * Trying to `send` values after its termination arise a [[ChannelTerminatedException]].
  * When one consumer reads the [[Terminated]] token, the channel is closed. Any subsequent
  * read will return `Left(Channel.Closed`.
  */
trait TerminableChannel[T] extends Channel[Terminable[T]]:
  def terminate()(using Async): Unit

object TerminableChannel:

  /** Creates a [[TerminableChannel]] backed to [[SyncChannel]]. */
  def ofSync[T: ClassTag]: TerminableChannel[T] = TerminableChannelImpl(SyncChannel())

  /** Creates a [[TerminableChannel]] backed to [[BufferedChannel]]. */
  def ofBuffered[T: ClassTag]: TerminableChannel[T] = TerminableChannelImpl(BufferedChannel())

  /** Creates a [[TerminableChannel]] backed to an [[UnboundedChannel]]. */
  def ofUnbounded[T: ClassTag]: TerminableChannel[T] = TerminableChannelImpl(UnboundedChannel())

  private class TerminableChannelImpl[T: ClassTag](c: Channel[Terminable[T]]) extends TerminableChannel[T]:
    opaque type Res[R] = Either[Channel.Closed, R]

    private var _terminated: Boolean = false

    override val readSource: Async.Source[Res[Terminable[T]]] =
      c.readSource.transformValuesWith {
        case Right(Terminated) => c.close(); Left(Channel.Closed)
        case v @ _ => v
      }

    override def sendSource(x: Terminable[T]): Async.Source[Res[Unit]] =
      synchronized:
        if _terminated then throw ChannelTerminatedException()
        else if x == Terminated then _terminated = true
      c.sendSource(x)

    override def close(): Unit = c.close()

    override def terminate()(using Async): Unit =
      try send(Terminated)
      // It happens only at the close of the channel due to the call (inside Gears 
      // library) of a CellBuf.dequeue(channels.scala:239) which is empty!
      catch case _: NoSuchElementException => ()
```

Now, also in Scala with Gears is possible to write:

```scala
val channel = TerminableChannel.ofUnbounded[Int]
Future:
  (0 until 10).foreach(channel.send(_))
  channel.terminate() // we're done sending
channel.foreach(println(_)) // blocks until channel is closed
println("Done!")
```

[[Other tests can be found in `TerminableChannelTest`]().]

On top of this new abstraction is possible to implement, for example, the `foreach` and `toSeq` methods, which can be useful to wait for all the items sent over the channel.

```scala
object TerminableChannelOps:

  extension [T: ClassTag](c: TerminableChannel[T])
    /** Blocking consume channel items, executing the given function [[f]] for each element. */
    @tailrec
    def foreach[U](f: T => U)(using Async): Unit = c.read() match
      case Left(Channel.Closed) => ()
      case Right(value) =>
        value match
          case Terminated => ()
          case v: T => f(v); foreach(f)

    /** @return a [[Seq]] containing channel items, after having them read.
      * This is a blocking operation! */
    def toSeq(using Async): Seq[T] =
      var results = Seq[T]()
      c.foreach(t => results = results :+ t)
      results
```

Three types of channels exist:

- **Synchronous Channels**: links a `read` request with a `send` within a _rendezvous_
  {{< figure src="../../res/img/sync-channel.svg" alt="synchronous channel" >}}
  - `send` (`read`) suspend the process until a consumer `read` (`send`) the value;
  - in Kotlin they are called **Rendezvous Channels**.
- **Buffered Channels**: a version of a channel with an internal buffer of fixed size
  {{< figure src="../../res/img/buffered-channel.svg" alt="buffered channel" >}}
  - `send` suspend the producer process if it is full; otherwise, it appends the value to the buffer, returning immediately;
  - `read` suspend if the channel is empty, waiting for a new value.
- **Unbounded Channels**: a version of a channel with an unbounded buffer
  {{< figure src="../../res/img/unbounded-channel.svg" alt="unbounded channel" >}}
  - if the programs run out of memory you can get an out-of-memory exception!
  - in Kotlin they are called **Unlimited Channel**.

Kotlin offers also a fourth type: the **Conflated Channel**, where every new element sent to it overwrites the previously sent one, *never blocking*, so that the receiver gets always the latest element.

Concerning channel behavior, it is important to note that:

{{< hint info >}}

1. Multiple producers can send data to the channel, as well as multiple consumers can read them, **but each element is handled only _once_, by _one_ of them**, i.e. consumers **compete** with each other for sent values;
2. Once the element is handled, it is immediately removed from the channel;
   * Channels are fair: `send` and `read` operations to channels are fair w.r.t. the order of their invocations from multiple threads (they are served in first-in first-out order).

{{< /hint >}}

## Analyzer example

To show channels in action an example has been prepared:

> **Idea**: we want to realize a little asynchronous library allowing clients to collect the common statistics about repositories (issues, stars, last release) and contributors of a given GitHub organization.

The final result is a GUI application that, given an organization name, starts the analysis of all its repositories,
listing their information along with all their contributors as soon as they are computed. Moreover, the application allows the user to cancel the current computation at any point in time.

![expected result](../../res/img/analyzer-e2e.png)

The example is structured in two different packages: `lib` and `client`. The former contains the logic of the library, while the latter contains the application (client code).
As usual, it has been implemented using monadic `Future`s, as well as using Scala Gears and Kotlin Coroutines.

### Future monadic version

[[The sources are available inside the `analyzer-monadic` submodule](https://github.com/tassiLuca/PPS-22-direct-style-experiments/tree/master/blog-ws-monadic/src/main/scala/io/github/tassiLuca/dse/blog).]

The entry point of the library is the `Analyzer` interface which takes in input the organization name and a function through which is possible to react to results while they are computed.

Since we want to achieve cancellation, the monadic version leverages Monix `Task`, which is returned by the `analyze` method wrapped in an `EitherT` monad transformer to allow handling errors functionally.

```scala
/** A generic analyzer of organization/group/workspace repositories. */
trait Analyzer:

  /** @return a [[EitherT]] encapsulating a [[Task]] that performs the analysis of the
    * [[organizationName]]'s repositories, providing the results incrementally to the
    * [[updateResults]] function.
    */
  def analyze(organizationName: String)(
      updateResult: RepositoryReport => Unit,
  ): EitherT[Task, String, Seq[RepositoryReport]]
```

To retrieve data from GitHub, a `RepositoryService` interface has been created, following the same pattern:

```scala
/** A service exposing functions to retrieve data from a central hosting repository service. */
trait RepositoryService:

  /** @return a [[EitherT]] encapsulating a [[Task]] which get all the repositories
    *         owned by [[organizationName]].
    */
  def repositoriesOf(organizationName: String): EitherT[Task, String, Seq[Repository]]

  /** @return a [[EitherT]] encapsulating a [[Task]] which get all the contributors
    *         of [[repositoryName]] owned by [[organizationName]].
    */
  def contributorsOf(organizationName: String, repositoryName: String): EitherT[Task, String, Seq[Contribution]]

  /** @return a [[EitherT]] encapsulating a [[Task]] which get the last release
    *         of [[repositoryName]] owned by [[organizationName]].
    */
  def lastReleaseOf(organizationName: String, repositoryName: String): EitherT[Task, String, Release]
```

The implementation of the `Analyzer` is shown in the following code snippet and performs the following steps:

1. first, the list of repositories is retrieved;
2. if no error occurs, the analysis of each repository is performed concurrently, thanks to the `parTraverse` operator;
3. the analysis of each repository consists of retrieving the contributors and the last release of the repository and then updating the result through the `updateResult` function. Since both the contributors and last release retrieval are independent of each other, they are performed concurrently, thanks to `Task.parZip2`.

```scala
override def analyze(organizationName: String)(
    updateResult: RepositoryReport => Unit,
): EitherT[Task, String, Seq[RepositoryReport]] =
  for
    repositories <- repositoryService.repositoriesOf(organizationName)
    reports <- repositories.parTraverse(r => EitherT.right(r.performAnalysis(updateResult)))
  yield reports

extension (r: Repository)
  private def performAnalysis(updateResult: RepositoryReport => Unit): Task[RepositoryReport] =
    val contributorsTask = repositoryService.contributorsOf(r.organization, r.name).value
    val releaseTask = repositoryService.lastReleaseOf(r.organization, r.name).value
    for
      result <- Task.parZip2(contributorsTask, releaseTask)
      report = RepositoryReport(r.name, r.issues, r.stars, result._1.getOrElse(Seq.empty), result._2.toOption)
      _ <- Task(updateResult(report))
    yield report
```

Client-side, when a new session is requested, the `Analyzer` is used to start the computation, during which the single reports are aggregated and the UI is updated.
Whenever desired, the current computation can be stopped by canceling the Monix `CancelableFuture` returned by the `runToFuture` method, through which the returned Task from the `Analyzer` is started.

```scala
class MonadicAppController extends AppController:

  import monix.execution.Scheduler.Implicits.global
  private val view = AnalyzerView.gui(this)
  private val analyzer = Analyzer.ofGitHub()
  private var currentComputation: Option[CancelableFuture[Unit]] = None

  view.run()

  override def runSession(organizationName: String): Unit =
    var organizationReport: OrganizationReport = (Map(), Set())
    val f = analyzer.analyze(organizationName) { report =>
      organizationReport = organizationReport.mergeWith(report)
      view.update(organizationReport)
    }.value.runToFuture.map { case Left(value) => view.error(value); case Right(_) => view.endComputation() }
    currentComputation = Some(f)

  override def stopSession(): Unit = currentComputation foreach (_.cancel())
```

### Scala Gears version

[[The sources are available inside the `analyzer-direct` submodule](https://github.com/tassiLuca/PPS-22-direct-style-experiments/tree/master/analyzer-direct/src/main/scala/io/github/tassiLuca/analyzer).]

The interfaces of the Direct Style with Gears differ from the monadic one by their return type, which is a simpler `Either` data type, and by the fact they are **suspendable functions**, hence they require an Async context to be executed.
This is the first important difference: the `analyze` method, differently from the monadic version, doesn't return immediately the control; instead, it suspends the execution of the client until the result is available (though offering the opportunity to react to each update).
This obeys the principle of **explicit asynchrony**: if the client wants to perform this operation asynchronously, it has to opt in explicitly, using a `Future`.

```scala
/** A generic analyzer of organization/group/workspace repositories. */
trait Analyzer:

  /** Performs a **suspending** analysis of the [[organizationName]]'s repositories,
    * providing the results incrementally to the [[updateResults]] function.
    * @return [[Right]] with the overall results of the analysis or [[Left]] with 
    *         an error message in case of failure.
    */
  def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]]
```

```scala
/** A service exposing functions to retrieve data from a central hosting repository service. */
trait RepositoryService:

  /** @return [[Right]] with the [[Seq]]uence of [[Repository]] owned by the given
    *         [[organizationName]] or a [[Left]] with a explanatory message in case of errors.
    */
  def repositoriesOf(organizationName: String)(using Async): Either[String, Seq[Repository]]

  /** @return [[Right]] with the [[Seq]]uence of [[Contribution]] for the given 
    *       [[repositoryName]] owned by the given [[organizationName]] or a [[Left]] with an 
    *       explanatory message in case of errors.
    */
  def contributorsOf(
    organizationName: String, 
    repositoryName: String
  )(using Async): Either[String, Seq[Contribution]]

  /** @return a [[Right]] with the last [[Release]] of the given [[repositoryName]] owned 
    *         by [[organizationName]] if it exists, or a [[Left]] with a explanatory message
    *         in case of errors.
    */
  def lastReleaseOf(
    organizationName: String, 
    repositoryName: String
  )(using Async): Either[String, Release]
```

The implementation of the `Analyzer` leverages Channels to perform the concurrent analysis of the repositories:

```scala
override def analyze(organizationName: String)(
    updateResults: RepositoryReport => Unit,
)(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]] = either:
  val reposInfo = repositoryService.repositoriesOf(organizationName).? // 1
    .map(_.performAnalysis.run) // 2
  val collector = Collector[RepositoryReport](reposInfo.toList*) // 3
  reposInfo.foreach(_ => 
    updateResults(collector.results.read().asTry.?.awaitResult.?) // 4
  )
  reposInfo.awaitAll // 5

extension (r: Repository)
  protected def performAnalysis(using Async): Task[RepositoryReport] = Task:
    val contributions = Future: // concurrent!
      repositoryService.contributorsOf(r.organization, r.name)
    val release = repositoryService.lastReleaseOf(r.organization, r.name)
    RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.toOption)
```

1. first, we get all the repositories of the requested organization
2. for each of them, the contributors and the last release are retrieved concurrently, starting a `Future`
3. `Future` results are gathered inside a **`Collector`** allowing to collect a list of futures into a channel of futures, **arriving as they finish**.
   * the retrieval of the contributors and the last release are performed in parallel
4. read results from the channel as they come, calling the `updateResult` reaction function.
5. Overall results are returned to the client.

Although it works, the proposed solution suffers from a performance issue when the organization we want to analyze has a large number of repositories.
Indeed, the GitHub API, like many ReSTful APIs, implements _pagination_: if the response includes many results, they are paginated, returning a subset of them; it is the responsibility of the client to request more data (pages).
Until now, the `RepositoryService` has been implemented to return the whole results in one shot, leading to suspension until all pages are retrieved.
It would be desirable, instead, to start performing the analysis as soon as one page is obtained from the API.

To do so, the interface of the `RepositoryService` has been extended with new methods, `incremental***`, returning a `TerminableChannel` of results:

```scala
trait RepositoryService:
  /** @return a [[Terminable]] [[ReadableChannel]] with the [[Repository]] owned by the given
    *         [[organizationName]], wrapped inside a [[Either]] for errors management.
    */
  def incrementalRepositoriesOf(
      organizationName: String,
  )(using Async): TerminableChannel[Either[String, Repository]]

  /** @return a [[Terminable]] [[ReadableChannel]] with the [[Contribution]] made by users to the
    *         given [[repositoryName]] owned by [[organizationName]], wrapped inside a [[Either]].
    */
  def incrementalContributorsOf(
      organizationName: String,
      repositoryName: String,
  )(using Async): TerminableChannel[Either[String, Contribution]]

  // ...

```

Then, the implementation of the `analyze` method becomes:

```scala
override def analyze(organizationName: String)(
    updateResults: RepositoryReport => Unit,
)(using Async): Either[String, Seq[RepositoryReport]] = either:
  val reposInfo = repositoryService.incrementalRepositoriesOf(organizationName) // 1
  var allReports = Seq[RepositoryReport]()
  var futures = Seq[Future[Unit]]()
  reposInfo.foreach { repository => // 2
    futures = futures :+ Future: // 3
      val report = repository.?.performAnalysis.awaitResult.?
      updateResults(report)
      allReports = allReports :+ report
  }
  futures.awaitAllOrCancel // 4
  allReports
```

1. we get the channel of repositories from the repository service;
2. the `foreach` method of `TerminableChannel` is used to iterate over all the repositories sent over the channel as soon as they are retrieved by the service. This is a blocking operation, i.e. it suspends until all the repositories are retrieved;
3. we start the analysis in a separate `Future` (i.e. thread): this allows you to start the analysis as soon as a repository is fetched by the channel, preventing starting the analysis of the next repository only when the previous one is finished;
4. once all the repositories are retrieved, i.e. the `foreach` terminates, we wait for the completion of all the started `Future`s. Indeed, when the `foreach` terminates, we have the guarantee that all started futures have been started, but not yet completed!

---

To start the application:

```bash
./gradlew analyzer-direct:run
```

{{< hint warning >}}

In order to use the application you need to place inside the `analyzer-commons` module a `.env` file containing [your personal GitHub access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens), like:

```env
GH_TOKEN=....
```

or having set an environment variable named `GH_TOKEN`.

{{< /hint >}}

---

### Kotlin Coroutines version

[[The sources are available inside the `analyzer-direct-kt` submodule](https://github.com/tassiLuca/PPS-22-direct-style-experiments/tree/master/analyzer-direct-kt/src/main/kotlin/io/github/tassiLuca/analyzer).]

The analyzer interface reflects the Scala Gears one: a `Result` is used in place of `Either`, and the suspendable function `udateResults` is marked with the `suspend` keyword in place of the `using Async` context.

```kotlin
interface Analyzer {
    suspend fun analyze(
        organizationName: String,
        updateResults: suspend (RepositoryReport) -> Unit = { _ -> },
    ): Result<Set<RepositoryReport>>
}
```

Its channel-based implementation, despite syntactic differences, is also very similar to that of Scala Gears, at least conceptually:

1. we get all the repositories;
2. for each of them, an analysis is started to retrieve the contributors and the last release;
    * each analysis is started in a separate coroutine whose results are sent to a channel;
    * as usual, the contributors and the last release are retrieved concurrently, using the `async` coroutine builder;
3. results are aggregated as they come from the channel.

```kotlin
override suspend fun analyze(
    organizationName: String,
    updateResults: suspend (RepositoryReport) -> Unit,
): Result<Set<RepositoryReport>> = coroutineScope {
    runCatching {
        val repositories = provider.repositoriesOf(organizationName).getOrThrow() // 1
        val resultsChannel = analyzeAll(organizationName, repositories) // 2
        collectResults(resultsChannel, repositories.size, updateResults) // 3
    }
}
```

```kotlin
private fun CoroutineScope.analyzeAll(organizationName: String, repositories: List<Repository>) =
    Channel<RepositoryReport>().also {
        repositories.map { r ->
            launch { // a new coroutine for each repository is started
                val contributors = async { provider.contributorsOf(organizationName, r.name).getOrThrow() }
                val release = provider.lastReleaseOf(organizationName, r.name).getOrThrow()
                it.send(RepositoryReport(r.name, r.issues, r.stars, contributors.await(), release))
            }
        }
    }
```

```kotlin
private suspend fun collectResults(
    resultsChannel: Channel<RepositoryReport>,
    expectedResults: Int,
    updateResults: suspend (RepositoryReport) -> Unit,
) = mutableSetOf<RepositoryReport>().apply {
    repeat(expectedResults) {
        val report = resultsChannel.receive()
        add(report)
        updateResults(report)
    }
    resultsChannel.close()
}
```

Where, instead, Kotlin Coroutines shine is the implementation of the `RepositoryService` for supporting incremental retrieval of repositories and contributors.

Indeed, Kotlin has a built-in support for cold streams, called **`Flow`**. They are very similar (actually they have been inspired to) cold observable in reactive programming, and **they are the perfect fit for functions that need to return a stream of asynchronously computed values**.

The `RepositoryService` has been here extended with new methods, `flowing***`, returning a `Flow` of results:

```kotlin
class GitHubRepositoryProvider {

  fun flowingRepositoriesOf(organizationName: String): Flow<List<Repository>>

  fun flowingContributorsOf(organizationName: String, repositoryName: String): Flow<List<Contribution>>
}
```

As already mentioned, the `Flow` is a **cold stream**, meaning that it is **not** started until it is **`collect`ed**. Once the `collect` method is called a new stream is created and data starts to "flow".

They offer several useful operators for transforming and combining them functionally (not a complete list):

{{< columns >}}

*Intermediate flow operators*:

- `filter`/`filterNot` to filter out unwanted values;
- `map` to transform the values;
- `transform` to implement more complex transformations (possibly involving suspending operations);
- `take` and its variant (e.g. `takeWhile`) to limit the number of values emitted;
- `onEach` to perform side-effects for each value emitted;

<--->

*Terminal flow operators*:

- conversions to various collection types, like `toList`, `toSet`;
- `first`, `last`, `single` to retrieve the first, last or single value emitted;
- `reduce` to perform some kind of operation over all items, reducing them to a single one;
- `fold` to perform some kind of operation over all items, starting from an initial value, accumulating a result;

<--->

*Flows combining operators*:

- `merge` to combine multiple flows into a single one, emitting values from all of them;
- `zip` 
- `combine` 
- `flatMapConcat` / `flatMapMerge` to transform each value into a flow and then concatenate/merge them;

{{< /columns >}}


Moreover, like in Rx:

- it is possible to control the context in which the flow is executed using the `flowOn` operator, which changes the context for all the steps above it (so it is typically used as the last step in a function);
- some backpressure strategies are supported, which are used to handle the situation when the producer is emitting values faster than the consumer can process them. In Kotlin Coroutines, the backpressure is managed by the `buffer` (and its variant, like `conflated`, `sample`, `debounce`) operator, which allows to buffer a certain number of values before the consumer starts to process them.

```kotlin
override suspend fun analyze(
    organizationName: String,
    updateResults: suspend (RepositoryReport) -> Unit,
): Result<Set<RepositoryReport>> = coroutineScope {
    runCatching {
        val reports = provider.flowingRepositoriesOf(organizationName)
            .flatMapConcat { analyzeAll(it) }
            .flowOn(Dispatchers.Default)
        var allReports = emptySet<RepositoryReport>() 
        // until here just "configuration"
        reports.collect {
            updateResults(it)
            allReports = allReports + it
        }
        allReports
    }
}
```

## Introducing `Flow`s in Gears

{{< hint info >}}

A similar abstraction of Kotlin `Flow`s can be implemented in Scala Gears leveraging `Task`s and `TerminableChannel`s.
The following section describes the attempt made to implement it and what has been achieved.

{{< /hint >}}

- When building the `Flow`, the client provides a block of code through which emits values, which is wrapped inside a `Task` that is started only when the `collect` method is called;
- The values are sent (emitted) on a `TerminableChannel` which is created when the `collect` method is called;
  - the behavior of the `emit` method is defined inside the `apply` method of `Flow` and injected inside caller code via the context parameter `(it: FlowCollector[T]) ?=>`.
- Once the task has finished, the channel is terminated.

[[Source code can be found in `commons` submodule, `pimpimg` package](https://github.com/tassiLuca/PPS-22-direct-style-experiments/blob/master/commons/src/main/scala/io/github/tassiLuca/pimping/Flow.scala).]

```scala
/** An asynchronous cold data stream that emits values, inspired to Kotlin Flows. */
trait Flow[+T]:

  /** Start the flowing of data which can be collected reacting through the given [[collector]] function. */
  def collect(collector: Try[T] => Unit)(using Async, AsyncOperations): Unit

/** An interface modeling an entity capable of [[emit]]ting [[Flow]]s values. */
trait FlowCollector[-T]:

  /** Emits a value to the flow. */
  def emit(value: T)(using Async): Unit

object Flow:

  /** Creates a new asynchronous cold [[Flow]] from the given [[body]].
    * Since it is cold, it starts emitting values only when the [[Flow.collect]] method is called.
    * To emit a value use the [[FlowCollector]] given instance.
    */
  def apply[T](body: (it: FlowCollector[T]) ?=> Unit): Flow[T] =
    val flow = FlowImpl[T]()
    flow.task = Task:
      val channel = flow.channel
      flow.sync.release()
      val collector: FlowCollector[T] = new FlowCollector[T]:
        override def emit(value: T)(using Async): Unit = channel.send(Success(value))
      try body(using collector)
      catch case e: Exception => channel.send(Failure(e))
    flow

  private class FlowImpl[T] extends Flow[T]:
    private[Flow] var task: Task[Unit] = uninitialized
    private[Flow] var channel: TerminableChannel[Try[T]] = uninitialized
    private[Flow] val sync = Semaphore(0)

    override def collect(collector: Try[T] => Unit)(using Async, AsyncOperations): Unit =
      val myChannel = TerminableChannel.ofUnbounded[Try[T]]
      synchronized:
        channel = myChannel
        task.run.onComplete(() => myChannel.terminate())
        // Ensure to leave the synchronized block after the task has been initialized
        // with the correct channel instance.
        sync.acquire()
      myChannel.foreach(t => collector(t))
```

`map` and `flatMap` have been implemented on top of `Flow`:

```scala
object FlowOps:

  extension [T](flow: Flow[T])

    /** @return a new [[Flow]] whose values has been transformed according to [[f]]. */
    def map[R](f: T => R): Flow[R] = new Flow[R]:
      override def collect(collector: Try[R] => Unit)(using Async, AsyncOperations): Unit =
        catchFailure(collector):
          flow.collect(item => collector(Success(f(item.get))))

    /** @return a new [[Flow]] whose values are created by flattening the flows generated
      *         by the given function [[f]] applied to each emitted value of this.
      */
    def flatMap[R](f: T => Flow[R]): Flow[R] = new Flow[R]:
      override def collect(collector: Try[R] => Unit)(using Async, AsyncOperations): Unit =
        catchFailure(collector):
          flow.collect(item => f(item.get).collect(x => collector(Success(x.get))))
```

### Showcasing `Flow`s

Library use cases:

```scala
type Name = String
type WriterId = Int
type Writer = (Name, WriterId)
type Book = String

object LibraryService:
  private val users: Set[Writer] = Set(("Alice", 987), ("Bob", 123), ("Charlie", 342))
  private val books: Map[WriterId, Set[Book]] = Map(
    987 -> Set("Alice's Adventures in Wonderland", "Through the Looking-Glass"),
    123 -> Set("The Shining"),
    342 -> Set("The Raven", "The Tell-Tale Heart"),
  )

  def allWriters(using Async): Flow[Writer] = Flow:
    users.foreach { u =>
      sleep(2_000)
      it.emit(u)
    }

  def booksByWriter(writer: WriterId)(using Async): Flow[Book] = Flow:
    books(writer).foreach(it.emit)
```

{{< columns >}}

**Flows are cold**

```scala
@main def useSimple(): Unit = Async.blocking:
  val service = LibraryService
  val writers = service.allWriters
  log(s"Not collecting yet!")
  sleep(1_000) // something meaningful
  log("Starting collecting users...")
  writers.collect(u => log(s"User: $u"))
  println("Done")
```

What we get is something like:

```plaintext
[1709559932492] Not collecting yet!
[1709559933500] Starting collecting users...
[1709559935532] User: Success((Alice,987))
[1709559937536] User: Success((Bob,123))
[1709559939541] User: Success((Charlie,342))
Done
```

If something goes wrong during the task execution, a `Failure` is emitted and the task terminates (no more values are emitted).
For example:

```scala
def failingWriters(using Async): Flow[Writer] = Flow:
  throw IllegalStateException("Library is closed")
  it.emit(users.head)

@main def useFailingFlow(): Unit = Async.blocking:
  val service = LibraryService
  val writers = service.failingWriters
  writers.collect(println)
```

Results in:

```plaintext
Failure(java.lang.IllegalStateException: 
  The library is closed)
```

<--->

**Flows can be transformed**

`map`ping `Writer`s to their identifier:

```scala
@main def useWithMapping(): Unit = Async.blocking:
  val service = LibraryService
  val writersId = service.allWriters.map(_._2)
  writersId.collect(a => println(s"Id: $a"))
```

Result:

```plaintext
Id: Success(987)
Id: Success(123)
Id: Success(342)
```

`flatMap`ping to get all the books:

```scala
@main def useWithFlatMap(): Unit = Async.blocking:
  val service = LibraryService
  val allBooks = service.allWriters.flatMap(w => 
    service.booksByWriter(w._2)
  )
  allBooks.collect(println)
```

Result:

```plaintext
Success(Alice's Adventures in Wonderland)
Success(Through the Looking-Glass)
Success(The Shining)
Success(The Raven)
Success(The Tell-Tale Heart)
```

{{< /columns >}}

👉🏻 [More tests on `Flows` can be found in `commons`, `pimping` pakcage](https://github.com/tassiLuca/PPS-22-direct-style-experiments/blob/master/commons/src/test/scala/io/github/tassiLuca/pimping/FlowTest.scala).

## Takeaways

> - `Channel`s are the basic communication and synchronization primitive for exchanging data between `Future`s/`Coroutine`s.
>   - Scala Gears support for `Terminable` channels or a review of the closing mechanism should be considered.
> - The `Flow` abstraction in Kotlin Coroutines is a powerful tool for handling cold streams of data, and it is a perfect fit for functions that need to return a stream of asynchronously computed values **by request**.
>   - A similar abstraction can be implemented in Scala Gears leveraging `Task`s and `TerminableChannel`s, enabling improved support for an asynchronous flow of data also in Gears, which is currently lacking.

{{< button relref="/02-basics" >}} **Previous**: Basic asynchronous constructs{{< /button >}}

{{< button relref="/04-rears" >}} **Next**: Reactivity in direct style{{< /button >}}
