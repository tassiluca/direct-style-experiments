package io.github.tassiLuca.analyzer.lib

import gears.async.{Async, AsyncOperations}
import io.github.tassiLuca.analyzer.commons.lib
import io.github.tassiLuca.analyzer.commons.lib.RepositoryReport

/** A generic analyzer of organization/group/workspace repositories. */
trait Analyzer:

  /** Performs a **suspending** analysis of the [[organizationName]]'s repositories, providing the results
    * incrementally to the [[updateResults]] function.
    * @return [[Right]] with the overall results of the analysis or [[Left]] with an error message in case of failure.
    */
  def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]]

object Analyzer:
  /** @return the basic version of the [[Analyzer]], i.e. the one performing suspending
    *         operation until all repositories are fetched.
    */
  def basic(service: RepositoryService): Analyzer = BasicAnalyzer(service)

  /** @return an incremental version of the [[Analyzer]] which reacts to each fetched repositories
    *         making the computation faster when dealing with a high number of repositories.
    */
  def incremental(service: RepositoryService): Analyzer = IncrementalAnalyzer(service)
