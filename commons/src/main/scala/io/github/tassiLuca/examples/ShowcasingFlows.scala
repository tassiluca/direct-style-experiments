package io.github.tassiLuca.examples

import gears.async.default.given
import gears.async.{Async, AsyncOperations}
import gears.async.AsyncOperations.sleep
import io.github.tassiLuca.pimping.Flow
import io.github.tassiLuca.pimping.FlowOps.map
import io.github.tassiLuca.pimping.FlowOps.flatMap

import scala.util.Random

object ShowcasingFlows:

  type Name = String
  type WriterId = Int
  type Writer = (Name, WriterId)
  type Book = String

  object LibraryService:
    private val users: Set[Writer] = Set(("Alice", 987), ("Bob", 123), ("Charlie", 342))
    private val books: Map[WriterId, Set[Book]] = Map(
      987 -> Set("Alice's Adventures in Wonderland", "Through the Looking-Glass"),
      123 -> Set("The Shining"),
      342 -> Set("The Raven", "The Tell-Tale Heart"),
    )

    def allWriters(using Async): Flow[Writer] = Flow:
      users.foreach { u =>
        sleep(2_000)
        it.emit(u)
      }

    def booksByWriter(writer: WriterId)(using Async): Flow[Book] = Flow:
      books(writer).foreach(it.emit)

    def failingWriters(using Async): Flow[Writer] = Flow:
      throw IllegalStateException("The library is closed")
      it.emit(users.head)

  @main def useSimple(): Unit = Async.blocking:
    val service = LibraryService
    val writers = service.allWriters
    log(s"Not collecting yet!")
    sleep(1_000) // something meaningful
    log("Starting collecting users...")
    writers.collect(u => log(s"User: $u"))
    println("Done")

  @main def useFailingFlow(): Unit = Async.blocking:
    val service = LibraryService
    val writers = service.failingWriters
    writers.collect(println)

  @main def useWithMapping(): Unit = Async.blocking:
    val service = LibraryService
    val writersId = service.allWriters.map(_._2)
    writersId.collect(a => println(s"Id: $a"))

  @main def useWithFlatMap(): Unit = Async.blocking:
    val service = LibraryService
    val allBooks = service.allWriters.flatMap(w => service.booksByWriter(w._2))
    allBooks.collect(println)

  private def log(msg: String): Unit = println(s"[${System.currentTimeMillis()}] $msg")
