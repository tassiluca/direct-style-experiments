package io.github.tassiLuca.boundaries

import scala.util.boundary
import scala.util.boundary.break

object BoundaryExamples extends App:

  def firstIndex[T](xs: List[T], elem: T): Int =
    boundary:
      for (x, i) <- xs.zipWithIndex do if x == elem then break(i)
      -1
