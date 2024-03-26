package io.github.tassiLuca.analyzer.lib

import gears.async.Async
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository}
import io.github.tassiLuca.dse.boundaries.CanFail
import io.github.tassiLuca.dse.pimping.{Flow, TerminableChannel}

/** A service exposing functions to retrieve data from a central hosting repository service. */
trait RepositoryService:

  /** Suspend the execution to get all the [[Repository]] owned by the given [[organizationName]].
    * It may fail along the way!
    * @return the [[Seq]]uence of [[Repository]].
    */
  def repositoriesOf(organizationName: String)(using Async, CanFail): Seq[Repository]

  /** @return a [[TerminableChannel]] with the [[Repository]] owned by the given
    *         [[organizationName]], wrapped inside a [[Either]] for errors management.
    */
  def incrementalRepositoriesOf(
      organizationName: String,
  )(using Async.Spawn): TerminableChannel[Either[String, Repository]]

  /** @return a cold [[Flow]] with the [[Repository]] owned by the given [[organizationName]]. */
  def flowingRepositoriesOf(organizationName: String)(using Async): Flow[Repository]

  /** Suspend the execution to get all the [[Contribution]] made by users to the given
    * [[repositoryName]] owned by [[organizationName]]. It may fail along the way!
    * @return the [[Seq]]uence of [[Contribution]].
    */
  def contributorsOf(organizationName: String, repositoryName: String)(using Async, CanFail): Seq[Contribution]

  /** @return a [[Terminable]] [[ReadableChannel]] with the [[Contribution]] made by users to the given
    *         [[repositoryName]] owned by [[organizationName]], wrapped inside a [[Either]] for errors management.
    */
  def incrementalContributorsOf(
      organizationName: String,
      repositoryName: String,
  )(using Async.Spawn): TerminableChannel[Either[String, Contribution]]

  /** Suspend the execution to get the last [[Release]] of the given [[repositoryName]]
    * owned by [[organizationName]]. It may fail along the way!
    * @return the last [[Release]] if it exists.
    */
  def lastReleaseOf(organizationName: String, repositoryName: String)(using Async, CanFail): Release

object RepositoryService:

  /** Creates an instance of [[RepositoryService]] for dealing with GitHub repositories. */
  def ofGitHub(): RepositoryService = GitHubRepositoryService()
