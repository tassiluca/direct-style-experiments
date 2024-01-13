package io.github.tassiLuca.posts

import gears.async.default.given
import gears.async.Async
import gears.async.AsyncOperations

import java.time.LocalTime
import scala.util.{Failure, Random, Success}

extension (component: String)
  def simulates(action: String)(using Async): Unit =
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] $action")
    AsyncOperations.sleep(Random.nextInt(10_000))
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] ended $action")

  def simulatesBlocking(action: String): Unit =
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] $action")
    Thread.sleep(Random.nextInt(10_000))
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] ended $action")
