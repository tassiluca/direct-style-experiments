package io.github.tassiLuca.rears

import gears.async.{Async, Listener, Task}

trait Observable[E]:
  def asRunnable: Task[Unit]
  def src: Async.Source[E] = BoundarySource

  private object BoundarySource extends Async.OriginalSource[E]:
    private var listeners = Set[Listener[E]]()

    override def poll(k: Listener[E]): Boolean = false

    override def dropListener(k: Listener[E]): Unit = synchronized:
      listeners = listeners - k

    override protected def addListener(k: Listener[E]): Unit = synchronized:
      listeners = listeners + k

    def notifyListeners(e: E): Unit = synchronized:
      listeners.foreach(_.completeNow(e, this))
      
trait Consumer[E]:
  def update(e: E): Task[Unit]
