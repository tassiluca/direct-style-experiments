package io.github.tassiLuca.pimping

import gears.async.Async.Source
import gears.async.{Channel, ChannelClosedException, Listener}

import scala.util.{Failure, Success, Try}

extension [T](e: Either[Channel.Closed, T])
  def toTry(): Try[T] = e match
    case Left(Channel.Closed) => Failure(ChannelClosedException())
    case Right(t) => Success[T](t)

given listenerConversion[T, U]: Conversion[() => U, Listener[T]] = f => Listener((_, _) => f())

given listenerDataConversion[T]: Conversion[T => Unit, Listener[T]] = f => Listener((t, _) => f(t))

given listenerDataSourceConversion[T]: Conversion[(T, Source[T]) => Unit, Listener[T]] = f => Listener(f)
