package io.github.tassiLuca.utils

import gears.async.{Async, Channel, ReadableChannel}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try, boundary}

object ChannelsPimping:

  case object Terminated

  type Terminated = Terminated.type

  type Terminable[T] = T | Terminated

  extension [T: ClassTag](c: ReadableChannel[Terminable[T]])
    def read()(using Async): Either[Channel.Closed | Terminated.type, T] =
      boundary:
        ???

  extension [T](e: Either[Channel.Closed, T])
    def tryable: Try[T] = e match
      case Left(Channel.Closed) => Failure(IllegalStateException("Closed Channel!"))
      case Right(t) => Success[T](t)
