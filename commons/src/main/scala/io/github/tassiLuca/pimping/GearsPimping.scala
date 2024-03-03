package io.github.tassiLuca.pimping

import gears.async.Async.Source
import gears.async.{Channel, ChannelClosedException, Listener}

import scala.util.{Failure, Success, Try}

/** A bunch of given instances operating on [[Channel]]s. */
object ChannelConversions:
  given resToTry[T]: Conversion[Either[Channel.Closed, T], Try[T]] = _.asTry

extension [T](e: Either[Channel.Closed, T])
  /** @return a [[Try]] with [[Failure]] of [[ChannelClosedException]], or a [[Right]] with the result. */
  def asTry: Try[T] = e match
    case Left(Channel.Closed) => Failure(ChannelClosedException())
    case Right(t) => Success[T](t)

/** A bunch of given instances to easily turn function suppliers and consumers to [[Listener]]. */
object ListenerConversions:
  given supplierToListener[T, U]: Conversion[() => U, Listener[T]] = f => Listener((_, _) => f())
  given dataConsumerToListener[T]: Conversion[T => Unit, Listener[T]] = f => Listener((t, _) => f(t))
  given dataSourceConsumerToListener[T]: Conversion[(T, Source[T]) => Unit, Listener[T]] = f => Listener(f)
