package io.github.tassiLuca.analyzer.lib

import gears.async.Async
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository}
import io.github.tassiLuca.dse.pimping.{Flow, TerminableChannel}

/** A service exposing functions to retrieve data from a central hosting repository service. */
trait RepositoryService:

  /** @return [[Right]] with the [[Seq]]uence of [[Repository]] owned by the given
    *         [[organizationName]] or a [[Left]] with a explanatory message in case of errors.
    */
  def repositoriesOf(organizationName: String)(using Async): Either[String, Seq[Repository]]

  /** @return a [[Terminable]] [[ReadableChannel]] with the [[Repository]] owned by the given
    *         [[organizationName]], wrapped inside a [[Either]] for errors management.
    */
  def incrementalRepositoriesOf(
      organizationName: String,
  )(using Async.Spawn): TerminableChannel[Either[String, Repository]]
  
  def flowingRepositoriesOf(organizationName: String)(using Async): Flow[Repository]

  /** @return [[Right]] with the [[Seq]]uence of [[Contribution]] for the given [[repositoryName]] owned by
    *         the given [[organizationName]] or a [[Left]] with a explanatory message in case of errors.
    */
  def contributorsOf(organizationName: String, repositoryName: String)(using Async): Either[String, Seq[Contribution]]

  /** @return a [[Terminable]] [[ReadableChannel]] with the [[Contribution]] made by users to the given
    *         [[repositoryName]] owned by [[organizationName]], wrapped inside a [[Either]] for errors management.
    */
  def incrementalContributorsOf(
      organizationName: String,
      repositoryName: String,
  )(using Async.Spawn): TerminableChannel[Either[String, Contribution]]

  /** @return a [[Right]] with the last [[Release]] of the given [[repositoryName]] owned by [[organizationName]]
    *         if it exists, or a [[Left]] with a explanatory message in case of errors.
    */
  def lastReleaseOf(organizationName: String, repositoryName: String)(using Async): Either[String, Release]

object RepositoryService:

  /** Creates an instance of [[RepositoryService]] for dealing with GitHub repositories. */
  def ofGitHub(): RepositoryService = GitHubRepositoryService()
