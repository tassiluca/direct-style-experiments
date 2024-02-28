package io.github.tassiLuca.analyzer.lib

import gears.async.Async
import io.github.tassiLuca.analyzer.commons.lib
import io.github.tassiLuca.analyzer.commons.lib.RepositoryReport

trait Analyzer:
  def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async): Either[String, Seq[RepositoryReport]]

object Analyzer:
  def basic(service: RepositoryService): Analyzer = BasicAnalyzer(service)

  def incremental(service: RepositoryService): Analyzer = IncrementalAnalyzer(service)
