/** A producer, i.e. a runnable entity producing items on a channel. */
trait Producer[E]:
  protected val channel: Channel[E] = UnboundedChannel()
  def asRunnable: Task[Unit]
  def publishingChannel: ReadableChannel[E] = channel.asReadable

/** A consumer, i.e. a runnable entity devoted to consume data from a channel. */
trait Consumer[E, S]:
  val listeningChannel: SendableChannel[Try[E]] = UnboundedChannel()
  def asRunnable(using Async.Spawn): Task[Unit] = Task:
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach(react)
  .schedule(RepeatUntilFailure())
  protected def react(e: Try[E])(using Async.Spawn): S

/** A mixin to turn consumer stateful. */
trait State[E, S](initialValue: S):
  consumer: Consumer[E, S] =>
  private var _state: S = initialValue
  def state: S = synchronized(_state)
  override def asRunnable(using Async.Spawn): Task[Unit] = Task:
    listeningChannel.asInstanceOf[Channel[Try[E]]].read().foreach: e =>
      synchronized:
        _state = react(e)
  .schedule(RepeatUntilFailure())