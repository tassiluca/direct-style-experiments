package io.github.tassiLuca.dse.effects

import language.experimental.saferExceptions
import scala.annotation.experimental

@experimental
object SaferExceptions extends App:

  class DivisionByZero extends Exception

  // or equivalently, CanThrow[DivisionByZero] ?=> Int
  def div(n: Int, m: Int)(using CanThrow[DivisionByZero]): Int = m match
    case 0 => throw DivisionByZero()
    case _ => n / m

  println:
    try div(10, 0)
    catch case _: DivisionByZero => "Division by zero"

  val values = (10, 1) :: (5, 2) :: (4, 2) :: (5, 1) :: Nil
  println:
    try values.map(div)
    catch case _: DivisionByZero => "Division by zero"
