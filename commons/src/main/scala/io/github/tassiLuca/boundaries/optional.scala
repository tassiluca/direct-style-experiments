package io.github.tassiLuca.boundaries

import scala.util.boundary
import scala.util.boundary.{Label, break}

object optional:
  inline def apply[T](inline body: Label[None.type] ?=> T): Option[T] =
    boundary(Some(body))

  extension [T](o: Option[T])
    inline def ?(using label: Label[None.type]): T =
      o.getOrElse(break(None))
