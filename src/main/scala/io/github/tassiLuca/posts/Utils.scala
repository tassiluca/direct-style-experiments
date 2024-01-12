package io.github.tassiLuca.posts

import gears.async.default.given
import gears.async.Async
import gears.async.AsyncOperations

import scala.util.{Failure, Random, Success}

extension (component: String)
  def simulates(action: String)(using Async): Unit =
    println(s"[$component - ${Thread.currentThread()}] $action")
    AsyncOperations.sleep(Random.nextInt(10_000))
    println(s"[$component - ${Thread.currentThread()}] ended $action")

  def simulatesBlocking(action: String): Unit =
    println(s"[$component - ${Thread.currentThread()}] $action")
    Thread.sleep(Random.nextInt(10_000))
    println(s"[$component - ${Thread.currentThread()}] ended $action")
