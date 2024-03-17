package io.github.tassiLuca.dse.examples

import gears.async.*
import gears.async.TaskSchedule.Every
import gears.async.default.given

import java.time.LocalTime
import scala.util.{Random, Try}

/** ChannelMultiplexer := active entity which constantly (i.e. loops, indeed the [[run]] method is blocking!!) reads the
  * producers channels' and forward the read value towards all the subscribers channels'. Order is guaranteed only per
  * producer. Consumers must loops to continually listen for new values!
  *
  * Remarks:
  *   - if producer channel is bounded, even with no consumers, it will continue producing items onto it (the
  *     multiplexer reads value, freeing up space into the buffer)
  *   - if a consumer start reading value after the producer started, missing some values, those values are gone => like
  *     "hot observables" in Rx.
  */
object UseChannelMultiplexer:

  type Item = String

  /** The number of producers. */
  val producers = 1

  /** The number of consumers. */
  val consumers = 1

  /** The size of the bounded channel. */
  val bufferSize = 10

  def scheduledProducer(name: String, period: Long): (Task[Unit], Channel[Item]) =
    val channel = BufferedChannel[Item](bufferSize)
    val producingTask = Task:
      val item = s"producer-$name ${Random.nextInt(10)}"
      println(s"[PRODUCER-$name - ${Thread.currentThread()} @ ${LocalTime.now()}] producing $item")
      channel.send(item) // possibly, blocking operation
      println(s"[PRODUCER-$name - ${Thread.currentThread()} @ ${LocalTime.now()}] produced")
    (producingTask.schedule(Every(period)), channel)

  def loopedConsumer(name: String): (Task[Unit], Channel[Try[Item]]) =
    val channel = BufferedChannel[Try[Item]](bufferSize)
    val consumingTask = Task:
      while (true)
        println(s"[CONSUMER-$name - ${Thread.currentThread()} @ ${LocalTime.now()}] Waiting for a new item...")
        val item = channel.read() // blocking operation
        println(s"[CONSUMER-$name - ${Thread.currentThread()} @ ${LocalTime.now()}] received $item")
    (consumingTask, channel)

  @main def useMultiplexer(): Unit = Async.blocking:
    val multiplexer = ChannelMultiplexer[Item]()
    Future:
      multiplexer.run() // blocking call until the multiplexer is closed => needs to be called on a new thread
    for i <- 0 until producers do
      val (producer, producerChannel) = scheduledProducer(i.toString, Random.nextLong(4_000))
      multiplexer.addPublisher(producerChannel.asReadable)
      producer.start()
    Thread.sleep(10_000)
    for i <- 0 until consumers do
      val (consumer, consumerChannel) = loopedConsumer(i.toString)
      multiplexer.addSubscriber(consumerChannel.asSendable)
      consumer.start()
    Thread.sleep(60_000)
