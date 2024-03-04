package io.github.tassiLuca.dse.boundaries

import scala.util.boundary
import scala.util.boundary.{Label, break}

/** Represents a computation that will hopefully return [[Some]]thing or simply [[None]] if it can't. */
object optional:

  /** Defines the boundary for an [[Option]] returning computation, whose [[body]] is given in input. */
  inline def apply[T](inline body: Label[None.type] ?=> T): Option[T] =
    boundary(Some(body))

  extension [T](o: Option[T])
    /** @return the enclosed [[Option]] object if defined, or break to the enclosing boundary with [[None]]. */
    inline def ?(using label: Label[None.type]): T =
      o.getOrElse(break(None))
