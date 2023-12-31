package io.github.tassiLuca.boundaries

import scala.util.boundary
import scala.util.boundary.break

object BoundaryTests extends App:

  def firstIndex[T](xs: List[T], elem: T): Int =
    boundary:
      for (x, i) <- xs.zipWithIndex do if x == elem then break(i)
      -1

  println(firstIndex(List(1, 2, 3, 4), 4))
  println(firstIndex(List(1, 2, 3, 4), 0))
