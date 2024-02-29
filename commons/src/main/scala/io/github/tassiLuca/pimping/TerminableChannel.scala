package io.github.tassiLuca.pimping

import gears.async.{Async, BufferedChannel, Channel, SyncChannel, UnboundedChannel}

import scala.annotation.tailrec
import scala.reflect.ClassTag

/** A token to be sent to a channel to signal that it has been terminated. */
case object Terminated

type Terminated = Terminated.type

type Terminable[T] = T | Terminated

trait TerminableChannel[T] extends Channel[Terminable[T]]:
  def terminate()(using Async): Unit

object TerminableChannel:

  def ofSync[T: ClassTag]: TerminableChannel[T] =
    TerminableChannelImpl(SyncChannel())

  def ofBuffered[T: ClassTag]: TerminableChannel[T] =
    TerminableChannelImpl(BufferedChannel())

  def ofUnbounded[T: ClassTag]: TerminableChannel[T] =
    TerminableChannelImpl(UnboundedChannel())

  private class TerminableChannelImpl[T: ClassTag](c: Channel[Terminable[T]]) extends TerminableChannel[T]:
    opaque type Res[R] = Either[Channel.Closed, R]

    private var _terminated: Boolean = false

    override val readSource: Async.Source[Res[Terminable[T]]] =
      c.readSource.transformValuesWith {
        case v @ Right(Terminated) =>
          c.close()
          v
        case v @ _ => v
      }

    override def sendSource(x: Terminable[T]): Async.Source[Res[Unit]] = x match
      case Terminated =>
        if synchronized(_terminated) then throw IllegalStateException("Channel already terminated!")
        else synchronized { _terminated = true }
        c.sendSource(x)
      case t => c.sendSource(t)

    override def close(): Unit = c.close()

    override def terminate()(using Async): Unit = c.send(Terminated)

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

    /** @return a [[Seq]] containing channel items, after having them read. This is a blocking operation. */
    def toSeq(using Async): Seq[T] =
      var results = Seq[T]()
      c.foreach(t => results = results :+ t)
      results
