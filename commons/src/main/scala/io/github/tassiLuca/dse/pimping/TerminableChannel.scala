package io.github.tassiLuca.dse.pimping

import gears.async.Channel.Closed
import gears.async.{Async, BufferedChannel, Channel, SyncChannel, UnboundedChannel}

import scala.annotation.tailrec
import scala.language.postfixOps
import scala.reflect.ClassTag

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
  def ofSync[T]: TerminableChannel[T] = TerminableChannelImpl(SyncChannel())

  /** Creates a [[TerminableChannel]] backed to [[BufferedChannel]]. */
  def ofBuffered[T]: TerminableChannel[T] = TerminableChannelImpl(BufferedChannel())

  /** Creates a [[TerminableChannel]] backed to an [[UnboundedChannel]]. */
  def ofUnbounded[T]: TerminableChannel[T] = TerminableChannelImpl(UnboundedChannel())

  private class TerminableChannelImpl[T](c: Channel[Terminable[T]]) extends TerminableChannel[T]:
    opaque type Res[R] = Either[Channel.Closed, R]

    private var _terminated: Boolean = false

    override val readSource: Async.Source[Res[Terminable[T]]] =
      c.readSource.transformValuesWith:
        case Right(Terminated) => c.close(); Left(Channel.Closed)
        case v @ _ => v

    override def sendSource(x: Terminable[T]): Async.Source[Res[Unit]] =
      synchronized:
        if _terminated then throw ChannelTerminatedException()
        else if x == Terminated then _terminated = true
      c.sendSource(x)

    override def close(): Unit = c.close()

    override def terminate()(using Async): Unit =
      try send(Terminated)
      // It happens only at the close of the channel due to the call (inside Gears library) of
      // a CellBuf.dequeue(channels.scala:239) which is empty!
      catch case _: NoSuchElementException => () // e.printStackTrace()

object TerminableChannelOps:

  extension [T: ClassTag](c: TerminableChannel[T])

    /** Consume channel items, executing the given function [[f]] for each element. This is a blocking operation. */
    @tailrec
    def foreach[U](f: T => U)(using Async): Unit = c.read() match
      case Left(Channel.Closed) => ()
      case Right(value) =>
        value match
          case Terminated => ()
          case v: T => f(v); foreach(f)

    /** @return a [[Seq]] containing channel items, after having them read. This is a blocking operation. */
    def toSeq(using Async): Seq[T] =
      var results = Seq[T]()
      c.foreach(t => results = results :+ t)
      results
