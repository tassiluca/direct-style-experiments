package io.github.tassiLuca.dse.blog.core

import gears.async.{Async, AsyncOperations}
import gears.async.default.given

import java.time.LocalTime
import scala.util.{Failure, Random, Success}

extension (component: String)
  def simulates(action: String, minDuration: Int = 0, maxDuration: Int = 5_000)(using Async): Unit =
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] $action")
    AsyncOperations.sleep(Random.nextInt(maxDuration) + minDuration)
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] ended $action")

  def simulatesBlocking(action: String, minDuration: Int = 0, maxDuration: Int = 5_000): Unit =
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] $action")
    Thread.sleep(Random.nextInt(maxDuration) + minDuration)
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] ended $action")
