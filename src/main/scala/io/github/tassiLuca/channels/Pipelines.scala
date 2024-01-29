package io.github.tassiLuca.channels

import gears.async.default.given
import gears.async.{Async, Channel, Future, ReadableChannel, UnboundedChannel}
import io.github.tassiLuca.boundaries.{either, optional}
import io.github.tassiLuca.boundaries.optional.?

import scala.language.postfixOps

object Pipelines:

  def produceNumbers()(using Async): ReadableChannel[Int] =
    val channel = UnboundedChannel[Int]()
    var x = 1
    Future {
      while true do
        channel.send(x)
        x += 1
    }
    channel.asReadable

  def square(c: ReadableChannel[Int])(using Async): ReadableChannel[Int] =
    val channel = UnboundedChannel[Int]()
    Future {
      optional {
        while true do channel.send(c.read().toOption.? * c.read().toOption.?)
      }
    }
    channel

  @main def useMain(): Unit = Async.blocking:
    val numbers = produceNumbers()
    val squares = square(numbers)
    for i <- 0 to 10000 do println(squares.read())
    println("Done!")
