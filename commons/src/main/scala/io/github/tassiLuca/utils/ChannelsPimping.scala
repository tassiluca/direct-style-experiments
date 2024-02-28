package io.github.tassiLuca.utils

import gears.async.{Async, Channel, ReadableChannel, SendableChannel}

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object ChannelsPimping:

  case object Terminated

  type Terminated = Terminated.type

  type Terminable[T] = T | Terminated

  extension [T](c: SendableChannel[Terminable[T]]) def terminate()(using Async): Unit = c.send(Terminated)

  extension [T: ClassTag](c: ReadableChannel[Terminable[T]])
    @tailrec
    def foreach[U](f: T => U)(using Async): Unit = c.read() match
      case Left(Channel.Closed) => ()
      case Right(value) =>
        value match
          case Terminated => ()
          case v: T => f(v); foreach(f)

    def toSeq(using Async): Seq[T] =
      var results = Seq[T]()
      c.foreach(t => results = results :+ t)
      results

  extension [T](e: Either[Channel.Closed, T])
    def tryable: Try[T] = e match
      case Left(Channel.Closed) => Failure(IllegalStateException("Closed Channel!"))
      case Right(t) => Success[T](t)
