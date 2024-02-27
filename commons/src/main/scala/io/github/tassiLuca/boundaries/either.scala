package io.github.tassiLuca.boundaries

import scala.util.{Failure, Success, Try, boundary}
import scala.util.boundary.{Label, break}

/** Represents a computation that will hopefully return a [[Right]] value, but might fail with a [[Left]] one. */
object either:

  /** Defines the boundary for the [[Either]] returning computation, whose [[body]] is given in input. */
  inline def apply[L, R](inline body: Label[Left[L, Nothing]] ?=> R): Either[L, R] =
    boundary(Right(body))

  /** Quickly break to the enclosing boundary with a [[Left]] filled with [[l]]. */
  inline def left[L, R](l: L)(using Label[Left[L, Nothing]]): Either[L, R] = break(Left(l))

  extension [L, R](e: Either[L, R])
    /** @return this [[Right]] value or break to the enclosing boundary with the [[Left]] value. */
    inline def ?(using Label[Left[L, Nothing]]): R = e match
      case Right(value) => value
      case Left(value) => break(Left(value))

  extension [R](t: Try[R])
    /** @return this [[Success]] value or break to the enclosing boundary with a [[Left]] containing the converted
      *         `Throwable` exception performed by the implicit [[converter]].
      */
    inline def ?[L](using Label[Left[L, Nothing]])(using converter: Conversion[Throwable, L]): R = t match
      case Success(value) => value
      case Failure(exception) => break(Left(converter(exception)))

/** An object encapsulating a collection of `Throwable` given converters. */
object EitherConversions:

  /** Converts a `Throwable` to a `String` with its message. */
  given Conversion[Throwable, String] = _.getMessage
