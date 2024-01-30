package io.github.tassiLuca.rears

trait Controller[E]:

  def parallel[E](
      sources: Set[Observable[E]],
      consumers: Set[Consumer[E]],
      transformation: PipelineTransformation[E] = identity,
  ) = ???

  def sequential[E](
      sources: Set[Observable[E]],
      consumers: Set[Consumer[E]],
      transformation: PipelineTransformation[E] = identity,
  ) = ???
