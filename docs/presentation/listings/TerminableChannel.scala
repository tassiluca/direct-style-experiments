/** A token to be sent to a channel to signal that it has been terminated. */
case object Terminated

type Terminated = Terminated.type

/** A union type of [[T]] and [[Terminated]]. */
type Terminable[T] = T | Terminated

/** Exception raised by [[TerminableChannel.send()]] on terminated channel. */
class ChannelTerminatedException extends Exception

/** A [[Channel]] that can be terminated, signalling no more items will be sent,
  * still allowing to consumer to read pending values.
  * Trying to `send` values after its termination arise a [[ChannelTerminatedException]].
  * When one consumer reads the [[Terminated]] token, the channel is closed. Any subsequent
  * read will return `Left(Channel.Closed`.
  */
trait TerminableChannel[T] extends Channel[Terminable[T]]:
  def terminate()(using Async): Unit
