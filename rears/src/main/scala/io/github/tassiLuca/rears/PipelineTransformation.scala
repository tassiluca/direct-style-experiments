package io.github.tassiLuca.rears

import concurrent.duration.{Duration, DurationInt}
import gears.async.Channel.{Closed, Res}
import gears.async.default.given
import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.{Async, Channel, Future, ReadableChannel, SendableChannel, Task, Timer, UnboundedChannel}
import scala.language.postfixOps

type PipelineTransformation[T, R] = ReadableChannel[T] => ReadableChannel[R]

// TODO: IMPROVE WITH SRC AND ? IN PLACE OF .toOption?
extension [T](r: ReadableChannel[T])(using Async)

  /** @return a new [[ReadableChannel]] whose elements passes the given predicate [[p]].
    *
    * Example:
    * <pre>
    * ----1---2-------3----4---5--6----7---8---9---10--->
    *     |   |       |    |   |  |    |   |   |   |
    * ----V---V-------V----V---V--V----V---V---V---V-----
    *                 filter(_ % 2 == 0)
    * --------|--------------|------|-------|---------|--
    *         V              V      V       V         V
    * --------2--------------4------6-------8--------10->
    * </pre>
    */
  def filter(p: T => Boolean): ReadableChannel[T] = fromNew[T] { c =>
    val value = r.read().toOption.get
    if p(value) then c.send(value)
  }

  /** @return a new [[ReadableChannel]] whose elements are emitted only after
    *         the given [[timespan]] has elapsed since the last emission.
    *
    * Example:
    * <pre>
    * ----1---2-------3----4---5--6-----7---8---9---10-->
    *     |   |       |    |   |  |     |   |   |   |
    *     V   V       V    V   V  V     V   V   V   V
    * T----------T----------T----------T----------T------
    *                 debounce(1 second)
    * ---------------------------------------------------
    *        |         |         |      |             |
    *        V         V         V      V             V
    * -------1---------3---------5------7------------10->
    * </pre>
    */
  def debounce(timespan: Duration): ReadableChannel[T] =
    var lastEmission: Option[Long] = None
    fromNew[T] { emitter =>
      val value = r.read().toOption.get
      val now = System.currentTimeMillis()
      if lastEmission.isEmpty || now - lastEmission.get >= timespan.toMillis then
        emitter.send(value)
        lastEmission = Some(now)
    }

  /** Groups the items emitted by a [[ReadableChannel]] according to the given [[keySelector]].
    * @return key-value pairs, where the keys are the set of results obtained from applying the
    *         [[keySelector]] coupled to a new [[ReadableChannel]] where only items belonging to
    *         that grouping are emitted.
    *
    * Example:
    * <pre>
    * ----1---2-3--4---5--6--->
    *     |   | |  |   |  |
    *     V   V V  V   V  V
    * -------------------------
    *        groupBy(_ % 2)
    * -------------------------
    *      \     \
    * ----false--true------------>
    *        1     2
    *         \     \
    *          \     4
    *           3     \
    *            \     \
    *             5     6
    * </pre>
    */
  def groupBy[K](keySelector: T => K): ReadableChannel[(K, ReadableChannel[T])] =
    var channels = Map[K, UnboundedChannel[T]]()
    fromNew[(K, UnboundedChannel[T])] { emitter =>
      val value = r.read().toOption.get
      val key = keySelector(value)
      if !channels.contains(key) then
        channels = channels + (key -> UnboundedChannel[T]())
        emitter.send(key -> channels(key))
      channels(key).send(value)
    }

  /** @return a new [[ReadableChannel]] whose elements are buffered in a [[List]] of size [[n]].
    *         If [[timespan]] duration is elapsed since last read the list is emitted
    *         with collected elements until that moment (default: 5 seconds).
    *
    * Example:
    * <pre>
    * ----1---2-3----4---5--6----7---8-------->
    *     |   | |    |   |  |    |   |
    *     V   V V    V   V  V    V   V
    * |---------|-----------|------------T-----
    *   buffer(n = 3, timespan = 5 seconds)
    * |---------|-----------|------------|---->
    *           V           V            V
    * ------[1, 2, 3]---[4, 5, 6]------[7, 8]->
    * </pre>
    */
  def buffer(n: Int, timespan: Duration = 5 seconds): ReadableChannel[List[T]] =
    var buffer = List[T]()
    fromNew[List[T]] { emitter =>
      val timer = Timer(timespan)
      Future { timer.run() }
      val value = Async.raceWithOrigin(r.readSource, timer.src).awaitResult
      timer.cancel()
      if value._2 == timer.src then
        emitter.send(buffer)
        buffer = List.empty
      else
        buffer = buffer :+ value._1.asInstanceOf[Either[Closed, T]].toOption.get
        if buffer.size == n then
          emitter.send(buffer)
          buffer = List.empty
    }

  /** @return a new [[ReadableChannel]] whose elements are buffered in a [[List]] of items
    *         if emitted within [[timespan]] duration after the first one (default: 5 seconds).
    *
    * Example:
    * <pre>
    * ----1---2-3-4---5--6--7----------8----------->
    *     |   | | |   |  |  |          |
    *     V   V V V   V  V  V          V
    * ----|--------T--|--------T-------|--------T---
    *      buffer(timespan = 5 seconds)
    * -------------|-----------|----------------|---
    *              V           V                V
    * -------[1, 2, 3, 4]--[5, 6, 7]-----------[8]->
    * </pre>  .
    */
  def bufferWithin(timespan: Duration = 5 seconds): ReadableChannel[List[T]] =
    var buffer = List[T]()
    fromNew[List[T]] { emitter =>
      val timer = Timer(timespan)
      buffer = buffer :+ r.read().toOption.get
      Future { timer.run() }
      val f = Future:
        val tf = Future { timer.src.awaitResult }
        val tr = Task {
          buffer = buffer :+ r.read().toOption.get
        }.schedule(RepeatUntilFailure()).run
        tr.altWithCancel(tf).awaitResult
      f.awaitResult
      emitter.send(buffer)
      buffer = List.empty
      timer.cancel()
    }

// IMPORTANT REMARK: if Async ?=> is omitted the body of the task is intended to be **not**
// suspendable, leading to the block of the context until the task has failed!
// See `TasksTest` in root project for more about the task scheduling behavior.
private def fromNew[T](
    transformation: Async ?=> SendableChannel[T] => Unit,
)(using Async): ReadableChannel[T] =
  val channel = UnboundedChannel[T]()
  Task {
    transformation(channel.asSendable)
  }.schedule(RepeatUntilFailure()).run
  channel.asReadable
