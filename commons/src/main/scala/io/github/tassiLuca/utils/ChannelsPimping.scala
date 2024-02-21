package io.github.tassiLuca.utils

import gears.async.{Async, Channel, ReadableChannel}

import scala.util.{Failure, Success, Try}

object ChannelsPimping:

  case object Terminated

  type Terminated = Terminated.type

  extension [T](c: ReadableChannel[T | Terminated])
    def readUntilTerminated()(using Async): Either[Channel.Closed | Terminated, T] = ???

  extension [T](e: Either[Channel.Closed, T])
    def tryable: Try[T] = e match
      case Left(Channel.Closed) => Failure(IllegalStateException("Closed Channel!"))
      case Right(t) => Success[T](t)
