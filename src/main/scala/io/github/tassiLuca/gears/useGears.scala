package io.github.tassiLuca.gears

import gears.async.{Async, Future}
import gears.async.default.given

@main def useGears(): Unit =
  Async.blocking:
    val sum = Future:
      val f1 = Future("Hello gears... ")
      val f2 = Future("I'm happy to work with you :)")
      f1.await + f2.await
    println(sum.await)
