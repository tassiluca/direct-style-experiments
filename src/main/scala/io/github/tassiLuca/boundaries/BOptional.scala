package io.github.tassiLuca.boundaries

import scala.util.boundary
import scala.util.boundary.{Label, break}

/*
 * Result in rust?
 */

object optional:
  inline def apply[T](inline body: Label[None.type] ?=> T): Option[T] =
    boundary(Some(body))

  extension [T](r: Option[T])
    inline def ?(using label: Label[None.type]): T = r match
      case Some(x) => x
      case _ => break(None)

@main def useBOptional(): Unit =
  import optional.*
  def firstColumn[T](xss: List[List[T]]): Option[List[T]] =
    optional:
      xss.map(_.headOption.?)

  println(firstColumn(List(List(1, 2, 3), List(3))))
