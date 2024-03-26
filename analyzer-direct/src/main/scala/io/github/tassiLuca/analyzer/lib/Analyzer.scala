package io.github.tassiLuca.analyzer.lib

import gears.async.{Async, AsyncOperations}
import io.github.tassiLuca.analyzer.commons.lib
import io.github.tassiLuca.analyzer.commons.lib.RepositoryReport
import io.github.tassiLuca.dse.boundaries.CanFail

/** A generic analyzer of organization/group/workspace repositories. */
trait Analyzer:

  /** Performs a **suspending** analysis of the [[organizationName]]'s repositories, providing the results
    * incrementally to the [[updateResults]] function. It may fail along the way!
    * @return the overall results of the analysis.
    */
  def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations, CanFail): Seq[RepositoryReport]

object Analyzer:
  /** @return the basic version of the [[Analyzer]], i.e. the one performing suspending
    *         operation until all repositories are fetched.
    */
  def basic(service: RepositoryService): Analyzer = BasicAnalyzer(service)

  /** @return an incremental version of the [[Analyzer]] which reacts to each fetched repositories
    *         making the computation faster when dealing with a high number of repositories.
    */
  def incremental(service: RepositoryService): Analyzer = IncrementalAnalyzer(service)

  /** @return a version of the [[Analyzer]] internally using the new Flow abstraction. */
  def flowing(service: RepositoryService): Analyzer = FlowingAnalyzer(service)
