package io.github.tassiLuca.boundaries

import scala.util.boundary
import scala.util.boundary.{Label, break}

object either:

  inline def apply[L, R](inline body: Label[Left[L, Nothing]] ?=> R): Either[L, R] =
    boundary(Right(body))

  extension [L, R](e: Either[L, R])
    inline def ?(using Label[Left[L, Nothing]]): R = e match
      case Right(value) => value
      case Left(value) => break(Left(value))
