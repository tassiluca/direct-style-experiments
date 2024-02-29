package io.github.tassiLuca.pimping

import gears.async.{Channel, ChannelClosedException}

import scala.util.{Failure, Success, Try}

object ChannelsPimping:
  extension [T](e: Either[Channel.Closed, T])
    def toTry(): Try[T] = e match
      case Left(Channel.Closed) => Failure(ChannelClosedException())
      case Right(t) => Success[T](t)
