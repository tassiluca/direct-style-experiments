package io.github.tassiLuca.boundaries

import scala.util.boundary
import scala.util.boundary.break
import optional.*

object BoundaryExamples extends App:

  def firstIndex[T](xs: List[T], elem: T): Int =
    boundary:
      for (x, i) <- xs.zipWithIndex do if x == elem then break(i)
      -1

  def firstColumn[T](xss: List[List[T]]): Option[List[T]] =
    optional:
      xss.map(_.headOption.?)
