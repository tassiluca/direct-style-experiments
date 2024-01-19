package io.github.tassiLuca

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.default.given
import gears.async.*

import java.lang.Thread.sleep
import java.time.LocalTime
import scala.concurrent.duration.DurationInt

object UseTimerSource extends App:

  def timerConsumer(c: ReadableChannel[Any]): Task[Unit] = Task {
    println(s"[CONSUMER] Waiting for a new item...")
    val item = c.read() // blocking!
    println(s"[CONSUMER - ${LocalTime.now()}] received $item")
  }.schedule(RepeatUntilFailure())

  def timerProducer(c: SendableChannel[Any])(using async: Async): Task[Unit] =
    val timer = Timer(5.seconds)
    Future { timer.run() }
    Task(c.send(timer.src.awaitResult)).schedule(RepeatUntilFailure())

  Async.blocking:
    val channel = BufferedChannel[Any](10)
    timerConsumer(channel.asReadable).run
    timerProducer(channel.asSendable).run
    sleep(21.seconds.toMillis)
