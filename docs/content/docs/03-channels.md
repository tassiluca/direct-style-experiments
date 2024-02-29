---
bookToc: false
---

# Channels as a communication primitive

The fourth, yet not mentioned, abstraction of both Kotlin Coroutines and Scala Gears is the **channel**.
Channels represent the **primitive communication and coordination means** to exchange `Future` (or coroutines in the case of Kotlin) results. They are, at least conceptually, very similar to a queue where it is possible to send (and receive) data -- basically, exploiting the ***producer-consumer*** pattern.

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

The channel is defined through three distinct interfaces: `SendableChannel[-T]`, `ReadableChannel[+T]` and `Channel[T]`, where the latter extends from both `SendableChannel` and `ReadableChannel`. Typically, a `Channel` is created and a `SendableChannel` and `ReadableChannel` instances are respectively provided to the producer and the consumer, restricting their access to it. The same, almost identical, design is present also in Kotlin Coroutines where `SendChannel` and `ReceiveChannel` take respectively over the Gears `SendableChannel` and `ReadableChannel`.

Moreover, `Channel` inherits from `java.io.Closable`, making them closable objects: once closed, they raise `ChannelClosedException` when attempting to write to them and immediately return a `Left(Closed)` when attempting to read from them, preventing the consumer from finishing reading all the values sent on the channel before its closing.
This is not the case for Kotlin Coroutines where closing a channel indicates that no more values are coming, but doesn't prevent consuming already sent values. Moreover, in Kotlin is possible to use a regular for loop to receive elements from a channel (blocking the coroutine):

```kotlin
val channel = Channel<Int>()
launch {
    for (x in 1..5) channel.send(x * x)
    channel.close() // we're done sending
}
for (y in channel) println(y) // blocks until channel is closed
println("Done!")
```

A similar behavior can be achieved also in Gears pimping the framework with the concept of `Terminable` channel. After all, closing a channel in coroutines is a matter of sending a special token to it: the iteration stops as soon as this token is received.

`TBD`

Three types of channels exists:

- **Synchronous Channels**: links a `read` request with a `send` within a _rendezvous_
  - `send` (`read`) suspend the process until a consumer `read` (`send`) the value;
  - in Kotlin they are called **Rendezvous Channels**.
- **Buffered Channels**: a version of a channel with an internal buffer of fixed size
  - `send` suspend the producer process if it is full; otherwise, it appends the value to the buffer, returning immediately;
  - `read` suspend if the channel is empty, waiting for a new value.
- **Unbounded Channels**: a version of a channel with an unbounded buffer
  - if the programs run out of memory you can get an out-of-memory exception!
  - in Kotlin they are called **Unlimited Channel**.

Kotlin offers also a fourth type: the **Conflated Channel**, where every new element sent to it overwirtes the previously sent one, *never blocking*, so that the receiver gets always the latest element.

Concerning channel behavior, it is important to note that:

> 1. Multiple producers can send data to the channel, as well as multiple consumers can read them, **but each element is handled only _once_, by _one_ of them**, i.e. consumers **compete** with each other for sent values;
> 2. Once the element is handled, it is immediately removed from the channel;
> 3. Fairness: `TBD`

## Analyzer example

To show channels in action an example has been prepared:

{{< hint info >}}
**Idea**: we want to realize a little asynchronous library allowing clients to collect the common statistics about repositories (issues, stars, last release) and contributors of a given GitHub organization.
{{< /hint >}}

The final result is a GUI application that, given an organization name, starts the analysis of all its repositories,
listing their information along with all their contributors as soon as they are computed. Moreover, the application allows the user to cancel the current computation at any point in time.

![expected result](../../res/img/analyzer-e2e.png)

The example is structured in two different packages: `lib` and `client`. The former contains the logic of the library, while the latter contains the application (client code).
As usual, it has been implemented using monadic `Future`s, as well as using Scala Gears and Kotlin Coroutines.

### Future monadic version

The entry point of the library is the `Analyzer` interface which takes in input the organization name and a function through which is possible to react to results while they are computed.

Since we want to achieve cancellation, the monadic version leverages Monix `Task`s which is returned by the `analyze` method wrapped in an `EitherT` monad transformer to allow handling errors functionally.

```scala
trait Analyzer:
  def analyze(organizationName: String)(
      updateResult: RepositoryReport => Unit,
  ): EitherT[Task, String, Seq[RepositoryReport]]
```

To retrieve data from GitHub, a `RepositoryService` interface has been created, following the same pattern:

```scala
trait RepositoryService:
  def repositoriesOf(organizationName: String): EitherT[Task, String, Seq[Repository]]
  def contributorsOf(organizationName: String, repositoryName: String): EitherT[Task, String, Seq[Contribution]]
  def lastReleaseOf(organizationName: String, repositoryName: String): EitherT[Task, String, Release]
```

The implementation of the `Analyzer` is shown in the following code snippet and performs the following steps:

1. first, the list of repositories is retrieved;
2. if no error occurred, the analysis of each repository is performed concurrently, thanks to the `Traverse` functor offered by Cats;
3. the analysis of each repository consists of retrieving the contributors and the last release of the repository and then updating the result through the `updateResult` function. Since both the contributors and last release retrieval are independent of each other, they are performed concurrently, thanks to `Task.parZip2`.

```scala
override def analyze(organizationName: String)(
    updateResult: RepositoryReport => Unit,
): EitherT[Task, String, Seq[RepositoryReport]] =
  for
    repositories <- gitHubService.repositoriesOf(organizationName) // 1
    reports <- repositories.traverse(r => EitherT.right(r.performAnalysis(updateResult))) // 2
  yield reports

extension (r: Repository)
  private def performAnalysis(updateResult: RepositoryReport => Unit): Task[RepositoryReport] =
    val contributorsTask = gitHubService.contributorsOf(r.organization, r.name).value
    val releaseTask = gitHubService.lastReleaseOf(r.organization, r.name).value
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
    }.value.runToFuture.map { case Left(value) => view.error(value); case _ => view.endComputation() }
    currentComputation = Some(f)

  override def stopSession(): Unit = currentComputation foreach (_.cancel())
```

### Scala Gears version

The interfaces of the Direct Style with Gears differ from the monadic one by their return type, which is a simpler `Either` data type, and by the fact they are **suspendable** functions, hence they require an Async context to be executed.

```scala
trait Analyzer:
  def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async): Either[String, Seq[RepositoryReport]]
```

```scala
trait RepositoryService:
  def repositoriesOf(organizationName: String)(using Async): Either[String, Seq[Repository]]
  def contributorsOf(
    organizationName: String, 
    repositoryName: String
  )(using Async): Either[String, Seq[Contribution]]
  def lastReleaseOf(organizationName: String, repositoryName: String)(using Async): Either[String, Release]
```

The implementation of the `Analyzer` leverages Channels to perform the concurrent analysis of the repositories:

```scala
override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async): Either[String, Seq[RepositoryReport]] = either:
    val reposInfo = repositoryService.repositoriesOf(organizationName).? // 1
      .map(_.performAnalysis) // 2
    val collector = Collector[RepositoryReport](reposInfo.toList*) // 3
    for _ <- reposInfo.indices do 
      updateResults(collector.results.read().toTry().?.awaitResult.?) // 4
    reposInfo.map(_.await)

  extension (r: Repository)
    private def performAnalysis(using Async): Future[RepositoryReport] = Future:
      val contributions = Future { repositoryService.contributorsOf(r.organization, r.name) } // concurrent
      val release = repositoryService.lastReleaseOf(r.organization, r.name)
      RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.toOption)
```

1. first, we get all the repositories of the requested organization
2. for each of them, the contributors and the last release are retrieved concurrently, starting a `Future`
3. `Future` results are gathered inside a **`Collector`** allowing to collect a list of futures into a channel of futures, arriving as they finish.
   * the retrieval of the contributors and the last release are performed in parallel
4. read results from the channel as they come, calling the `updateResult` reaction function.

Although it works, the proposed solution suffers from a performance issue when the organization we want to analyze has a large number of repositories.
Indeed, the GitHub API, like many ReSTful APIs, implements _pagination_: if the response includes many results, they are paginated, returning a subset of them; it is the responsibility of the client to request more data (pages).
Until now, the `RepositoryService` has been implemented to return the whole results in one shot, leading to suspension until all pages are retrieved.
It would be desirable, instead, to start performing the analysis as soon as one page is obtained from the API.

To do so, the interface of the `RepositoryService` has been extended with new methods, `incremental***`, returning a terminable channel of results:

```scala
trait RepositoryService:
  def incrementalRepositoriesOf(
      organizationName: String,
  )(using Async): ReadableChannel[Terminable[Either[String, Repository]]]

  def incrementalContributorsOf(
      organizationName: String,
      repositoryName: String,
  )(using Async): ReadableChannel[Terminable[Either[String, Contribution]]]

  // ...

```



---
<!--
Client-side the code is quite identical to the corresponding monadic version:

```scala
class DirectAppController(using Async) extends AppController:
  private val view = AnalyzerView.gui(this)
  private val analyzer = Analyzer.incremental(RepositoryService.ofGitHub())
  private var currentComputation: Option[Future[Unit]] = None

  view.run()

  override def runSession(organizationName: String): Unit =
    var organizationReport: OrganizationReport = (Map(), Set())
    val f = Future:
      analyzer.analyze(organizationName) { report =>
        organizationReport = organizationReport.mergeWith(report)
        view.update(organizationReport)
      } match { case Left(e) => view.error(e); case _ => view.endComputation() }
    currentComputation = Some(f)

  override def stopSession(): Unit = currentComputation.foreach(_.cancel())
```

The Kotlin version with Coroutines is pretty identical to the Gears one.

### GitHub service

One point of difference between the two frameworks is, however, how the `GitHubService` can be implemented, regarding the case where, not just a single, but a multitude of values, are expected.

Indeed, the GitHub API, like many ReSTful APIs, implements _pagination_: if the response includes many results, they are paginated, returning a subset of them; it is the responsibility of the client to request more data (pages).

This can lead to performance issues if the service is implemented to return the whole results in one shot. It would be desirable, instead, to start performing the analysis as soon as one page is obtained from the API.

Kotlin Coroutines offers, for this purpose, the abstraction of **Flow**s, which are conceptually very similar to **cold `Observable`s** in reactive programming.

With respect to reactive programming, they are still quite less reach in terms of operators.

---

- Channels in Kotlin (w.r.t. gears)
  - fairness (also in Gears?)
  - pipeline (not supported in Gears)
  - better closable

---

Points of difference between the gears and Kotlin Coroutines channels are the following:

---

-->

## Conclusions
