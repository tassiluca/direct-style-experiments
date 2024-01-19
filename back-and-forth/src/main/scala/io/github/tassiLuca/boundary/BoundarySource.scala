package io.github.tassiLuca.boundary

import gears.async.{Async, Listener}
import io.github.tassiLuca.core.Event

object BoundarySource extends Async.OriginalSource[Event]:
  private[boundary] var listeners = Set[Listener[Event]]()

  override def poll(k: Listener[Event]): Boolean = false

  override def dropListener(k: Listener[Event]): Unit = synchronized:
    listeners = listeners - k

  override protected def addListener(k: Listener[Event]): Unit = synchronized:
    listeners = listeners + k

  def notifyListeners(e: Event): Unit = synchronized:
    listeners.foreach(_.completeNow(e, this))
