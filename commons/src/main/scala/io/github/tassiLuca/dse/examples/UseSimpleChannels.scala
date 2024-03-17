package io.github.tassiLuca.dse.examples

import gears.async.*
import gears.async.AsyncOperations.sleep
import gears.async.TaskSchedule.Every
import gears.async.default.given

import java.time.LocalTime

/** A usage examples of channels. Remarks:
  *   - consumers are competing with each other for sent values
  *   - each value sent to the channel gets delivered to **exactly** 1 consumer
  *   - consumers after consumes 1 items only, i.e. it's not like a subscribe in reactive programming!
  *     - Variant: perform a loop inside the consumer
  */
object UseSimpleChannels:

  type Item = Int

  /** The rate at which the producer produces a new item. */
  val producerPeriod = 4_000

  /** The number of consumers. */
  val consumers = 2

  /** The size of the bounded channel. */
  val bufferSize = 10

  val channel: BufferedChannel[Int] = BufferedChannel(bufferSize)

  /** A generic producer of items that produces a different item every. */
  def scheduledProducer(c: SendableChannel[Item]): Task[Unit] =
    var item = 0
    val producingTask = Task:
      println(s"[PRODUCER - ${Thread.currentThread()} @ ${LocalTime.now()}] producing $item")
      c.send(item) // possibly, blocking operation
      println(s"[PRODUCER - ${Thread.currentThread()} @ ${LocalTime.now()}] produced")
      item += 1
    producingTask.schedule(Every(producerPeriod))

  /** A generic consumer of items. */
  def consumer(c: ReadableChannel[Int]): Task[Unit] = Task:
    while (true)
      println(s"[CONSUMER - ${Thread.currentThread()} @ ${LocalTime.now()}] Waiting for a new item...")
      val item = c.read() // blocking operation
      println(s"[CONSUMER - ${Thread.currentThread()} @ ${LocalTime.now()}] received $item")

  @main def useChannels(): Unit = Async.blocking:
    for _ <- 0 until consumers do consumer(channel.asReadable).start()
    sleep(10_000)
    scheduledProducer(channel.asSendable).start()
    sleep(30_000)
