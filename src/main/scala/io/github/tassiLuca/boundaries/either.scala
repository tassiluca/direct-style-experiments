package io.github.tassiLuca.boundaries

import scala.util.{Failure, Success, Try, boundary}
import scala.util.boundary.{Label, break}

object either:

  inline def apply[L, R](inline body: Label[Left[L, Nothing]] ?=> R): Either[L, R] =
    boundary(Right(body))

  extension [L, R](e: Either[L, R])
    inline def ?(using Label[Left[L, Nothing]]): R = e match
      case Right(value) => value
      case Left(value) => break(Left(value))

  type ThrowableConverter[L] = Throwable => L

  extension [R](t: Try[R])
    inline def ?[L](using Label[Left[L, Nothing]])(using converter: ThrowableConverter[L]): R = t match
      case Success(value) => value
      case Failure(exception) => break(Left(converter(exception)))
