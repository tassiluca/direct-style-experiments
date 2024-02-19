package io.github.tassiLuca.dse.blog.core

import gears.async.default.given
import gears.async.{Async, AsyncOperations}

import java.time.LocalTime
import scala.util.Random

extension (component: String)
  /** A simple function simulating a <strong>suspending</strong> call that suspends its execution from a minimum of
    * [[minDuration]] to a maximum of [[maxDuration]] milliseconds.
    */
  def simulates(action: String, minDuration: Int = 0, maxDuration: Int = 3_000)(using Async): Unit =
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] $action")
    AsyncOperations.sleep(Random.nextInt(maxDuration) + minDuration)
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] ended $action")

  /** A simple function simulating a <strong>blocking</strong> call that blocks the current thread
    * from a minimum of [[minDuration]] to a maximum of [[maxDuration]] milliseconds.
    */
  def simulatesBlocking(action: String, minDuration: Int = 0, maxDuration: Int = 3_000): Unit =
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] $action")
    Thread.sleep(Random.nextInt(maxDuration) + minDuration)
    println(s"[$component - ${Thread.currentThread()} @ ${LocalTime.now()}] ended $action")

/** A simple enumeration representing the checks that can be performed on a blog post. */
enum Check:
  /** The content verification check. */
  case ContentVerified

  /** The author verification check. */
  case AuthorVerified

/** A trait representing keeping track of the checks performed. */
trait CheckFlag:
  def completedChecks: Set[Check]
