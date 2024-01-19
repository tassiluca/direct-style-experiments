package io.github.tassiLuca.utils

import scala.annotation.targetName

type Point2D = (Int, Int)

type Vector2D = (Double, Double)

extension (p: Point2D)

  @targetName("divideBy")
  def /(i: Int): Point2D = (p._1 / i, p._2 / i)

  @targetName("subtract")
  def -(i: Int): Point2D = (p._1 - i, p._2 - i)

extension (v: Vector2D) def module: Double = Math.sqrt(v._1 * v._1 + v._2 * v._2)
