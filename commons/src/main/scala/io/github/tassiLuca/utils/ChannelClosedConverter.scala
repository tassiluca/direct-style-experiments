package io.github.tassiLuca.utils

import gears.async.Channel

import scala.util.{Failure, Success, Try}

object ChannelClosedConverter:
  extension [T](e: Either[Channel.Closed, T])
    def tryable: Try[T] = e match
      case Left(Channel.Closed) => Failure(IllegalStateException("Closed Channel!"))
      case Right(t) => Success[T](t)
