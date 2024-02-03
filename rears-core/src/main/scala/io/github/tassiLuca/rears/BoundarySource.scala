package io.github.tassiLuca.rears

import gears.async.{Async, Listener}

class BoundarySource[E] extends Async.OriginalSource[E]:
  private var listeners = Set[Listener[E]]()

  override def poll(k: Listener[E]): Boolean = false

  override def dropListener(k: Listener[E]): Unit = synchronized:
    listeners = listeners - k

  override protected def addListener(k: Listener[E]): Unit = synchronized:
    listeners = listeners + k

  def notifyListeners(e: E): Unit = synchronized:
    // println(listeners)
    listeners.foreach(_.completeNow(e, this))
