package io.github.tassiLuca.boundaries.examples

import io.github.tassiLuca.boundaries.optional
import io.github.tassiLuca.boundaries.optional.?

import scala.util.boundary
import scala.util.boundary.break

object BoundaryExamples extends App:

  def firstIndex[T](xs: List[T], elem: T): Int =
    boundary:
      for (x, i) <- xs.zipWithIndex do if x == elem then break(i)
      -1

  def firstColumn[T](xss: List[List[T]]): Option[List[T]] =
    optional:
      xss.map(_.headOption.?)

  def firstColumn2[T](xss: List[List[T]]): Option[List[T]] =
    val firstElements = xss.map(_.headOption)
    if firstElements.forall(_.isDefined) then Some(firstElements.flatten) else None