package io.github.tassiLuca.core

import scala.annotation.targetName

type Reaction[Event, Model] = (Event, Model) => Model

extension [A, B, C](f: (A, B) => C)
  @targetName("chainWith")
  def >>[D](g: (A, C) => D): (A, B) => D = (a, b) => g(a, f(a, b))
