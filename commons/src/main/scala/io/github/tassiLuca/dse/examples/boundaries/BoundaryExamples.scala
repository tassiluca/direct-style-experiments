package io.github.tassiLuca.dse.examples.boundaries

import io.github.tassiLuca.dse.boundaries.optional.?
import io.github.tassiLuca.dse.boundaries.optional

import scala.annotation.tailrec
import scala.util.boundary
import scala.util.boundary.break

object BoundaryExamples extends App:

  // Non local returns are no longer supported; use `boundary` and `boundary.break` instead
  def deprecatedFirstIndex[T](xs: List[T], elem: T): Int =
    for (x, i) <- xs.zipWithIndex do if x == elem then return i
    -1

  def firstIndex[T](xs: List[T], elem: T): Int =
    boundary:
      for (x, i) <- xs.zipWithIndex do if x == elem then break(i)
      -1

  def functionalFirstIndex[T](xs: List[T], elem: T): Int =
    @tailrec
    def recur(xs: List[T], elem: T)(current: Int = 0): Int = xs match
      case h :: _ if h == elem => current
      case _ :: t => recur(t, elem)(current + 1)
      case _ => -1
    recur(xs, elem)()

  def firstColumn[T](xss: List[List[T]]): Option[List[T]] =
    optional:
      xss.map(_.headOption.?)
