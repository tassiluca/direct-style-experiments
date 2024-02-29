package io.github.tassiLuca.pimping

import gears.async.{Async, Channel, ReadableChannel, SendableChannel}

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object ChannelsPimping:

  /** A token to be sent to a channel to signal that it has been terminated. */
  case object Terminated

  type Terminated = Terminated.type

  type Terminable[T] = T | Terminated

  extension [T](c: SendableChannel[Terminable[T]])
    /** Terminates this channel, i.e. send to it a [[Terminated]] token. */
    def terminate()(using Async): Unit = c.send(Terminated)

  extension [T: ClassTag](c: ReadableChannel[Terminable[T]])

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

  extension [T](e: Either[Channel.Closed, T])
    def tryable: Try[T] = e match
      case Left(Channel.Closed) => Failure(IllegalStateException("Closed Channel!"))
      case Right(t) => Success[T](t)
