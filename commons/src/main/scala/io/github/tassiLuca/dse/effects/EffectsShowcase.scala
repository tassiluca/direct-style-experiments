package io.github.tassiLuca.dse.effects

import gears.async.AsyncOperations.sleep
import gears.async.default.given
import gears.async.{Async, AsyncOperations, Future}
import io.github.tassiLuca.dse.boundaries.EitherConversions.given
import io.github.tassiLuca.dse.boundaries.either.?
import io.github.tassiLuca.dse.boundaries.{CanFail, either}

import scala.util.boundary.Label

object EffectsShowcase extends App:

  def f(using Async, CanFail): String =
    Async.group:
      val f1 = Future:
        sleep(1_000)
        "hello"
      val f2 = Future:
        "world"
      f1.awaitResult.? + " " + f2.awaitResult.?

  def g(f: => String): String =
    "g: " + f

  @main def useEffectfulComputation(): Unit =
    Async.blocking:
      print:
        either:
          f

  @main def useG(): Unit =
    Async.blocking:
      print:
        either:
          g(f)
