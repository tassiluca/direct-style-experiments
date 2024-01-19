package io.github.tassiLuca.core

import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, Future, ReadableChannel, SendableChannel, Task, UnboundedChannel}
import io.github.tassiLuca.boundary.{Boundary, UpdatableBoundary}

import java.time.LocalTime
import scala.annotation.tailrec

trait Controller[Event, Model]:
  def reactive(
      boundaries: Set[Boundary],
      updatableBoundaries: Set[UpdatableBoundary[Model]],
      reaction: Reaction[Event, Model],
  )(initial: Space2D & RectangularEntities): Task[Unit]

object Controller extends Controller[Event, Space2D & RectangularEntities]:
  override def reactive(
      boundaries: Set[Boundary],
      updatableBoundaries: Set[UpdatableBoundary[Space2D & RectangularEntities]],
      reaction: Reaction[Event, Space2D & RectangularEntities],
  )(initial: Space2D & RectangularEntities): Task[Unit] = Task:
    val channel = UnboundedChannel[Event]() // may go out of memory!
    val producers = (boundaries ++ updatableBoundaries).map(_ produceOn channel)
    Future { consumeFrom(updatableBoundaries, channel, initial, reaction) }
    producers.map(_.run).toSeq.awaitAll // important!

  extension (b: Boundary)
    private def produceOn(channel: SendableChannel[Event]): Task[Unit] = Task {
      channel.send(b.src.awaitResult)
    }.schedule(RepeatUntilFailure())

  @tailrec
  private def consumeFrom(
      updatableBoundaries: Set[UpdatableBoundary[Space2D & RectangularEntities]],
      channel: ReadableChannel[Event],
      space2D: Space2D & RectangularEntities,
      reaction: Reaction[Event, Space2D & RectangularEntities],
  )(using Async): Unit =
    val event = channel.read().toOption.get // may fail due to closed channel
    println(s"Consuming $event @ ${LocalTime.now()}")
    val newSpace = reaction(event, space2D)
    updatableBoundaries.foreach(_.update(newSpace))
    consumeFrom(updatableBoundaries, channel, space2D, reaction)
