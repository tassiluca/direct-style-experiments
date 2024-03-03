package io.github.tassiLuca.rears

import gears.async.Channel.{Closed, Res}
import gears.async.TaskSchedule.RepeatUntilFailure
import gears.async.default.given
import gears.async.{Async, Channel, Future, ReadableChannel, SendableChannel, Task, Timer, UnboundedChannel}
import io.github.tassiLuca.pimping.ChannelConversions.given

import scala.concurrent.duration.{Duration, DurationInt}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/** Simply, a function that, given in input a [[ReadableChannel]], performs some
  * kind of transformation, returning, as a result, another [[ReadableChannel]].
  */
type PipelineTransformation[T, R] = ReadableChannel[T] => ReadableChannel[R]

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
  def filter(p: T => Boolean): ReadableChannel[T] = fromNew[T] { emitter =>
    val value = r.read().get
    if p(value) then emitter.send(value)
  }

  /** @return a new [[ReadableChannel]] whose values are transformed accordingly to the given function [[f]].
    *
    * Example:
    * <pre>
    * ----1---2-------3----4---5------6--------7-------->
    *     |   |       |    |   |      |        |
    * ----V---V-------V----V---V------V--------V---------
    *                 map(x => x * x)
    * ----|---|-------|----|---|------|--------|---------
    *     V   V       V    V   V      V        V
    * ----1---4-------9----16--25-----36-------49------->
    * </pre>
    */
  def map[R](f: T => R): ReadableChannel[R] = fromNew[R] { emitter =>
    emitter.send(f(r.read().get))
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
      val value = r.read().get
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
      val value = r.read().get
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
    * |---------|-----------|------------|-----
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
        buffer = buffer :+ value._1.asInstanceOf[Either[Closed, T]].get
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
    *          buffer(timespan = 5 seconds)
    * -------------|-----------|----------------|---
    *              V           V                V
    * -------[1, 2, 3, 4]--[5, 6, 7]-----------[8]->
    * </pre>
    */
  def bufferWithin(timespan: Duration = 5 seconds): ReadableChannel[List[T]] =
    var buffer = List[T]()
    fromNew[List[T]] { emitter =>
      val timer = Timer(timespan)
      buffer = buffer :+ r.read().get
      Future { timer.run() }
      val f = Future:
        val tf = Future { timer.src.awaitResult }
        val tr = Task {
          buffer = buffer :+ r.read().get
        }.schedule(RepeatUntilFailure()).run
        tr.altWithCancel(tf).awaitResult
      f.awaitResult
      emitter.send(buffer)
      buffer = List.empty
      timer.cancel()
    }

// IMPORTANT REMARK: if Async ?=> is omitted the body of the task is intended to be **not**
// suspendable, leading to the block of the context until the task fails!
// See `TasksTest` in commons tests for more about the task scheduling behavior.
private def fromNew[T](
    transformation: Async ?=> SendableChannel[T] => Unit,
)(using Async): ReadableChannel[T] =
  val channel = UnboundedChannel[T]()
  Task {
    Try(transformation(channel.asSendable)) match
      case s @ Success(_) => s
      case f @ Failure(_) => channel.close(); f
  }.schedule(RepeatUntilFailure()).run
  channel.asReadable
