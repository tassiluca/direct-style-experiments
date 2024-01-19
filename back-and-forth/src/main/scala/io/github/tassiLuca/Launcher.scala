package io.github.tassiLuca

import gears.async.default.given
import gears.async.Async

import concurrent.duration.DurationInt
import io.github.tassiLuca.boundary.impl.{SwingUI, Timer}
import io.github.tassiLuca.core.{Controller, Space2D}

import scala.language.postfixOps

@main def main(): Unit = Async.blocking:
  val view = SwingUI(500, 800)
  val timer = Timer(1 seconds)
  view.asRunnable.run
  timer.asRunnable.run
  val controller = Controller
    .reactive(
      boundaries = Set(timer),
      updatableBoundaries = Set(view),
      reaction = (_, s) => s,
    )(Space2D((450, 700)))
    .run
  controller.await
