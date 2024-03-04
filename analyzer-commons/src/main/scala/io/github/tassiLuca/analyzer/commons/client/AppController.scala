package io.github.tassiLuca.analyzer.commons.client

import io.github.tassiLuca.analyzer.commons.lib.RepositoryReport

/** A report for an organization, namely a [[Map]] of the overall its contributions
  * and a [[Set]] of its repositories.
  */
type OrganizationReport = (Map[String, Long], Set[RepositoryReport])

/** The analyzer application controller. */
trait AppController:
  /** Start a new analysis session of the given [[organizationName]]. */
  def runSession(organizationName: String): Unit

  /** Stops the ongoing analysis session. */
  def stopSession(): Unit

  extension (organizationReport: OrganizationReport)
    /** Merge the given [[report]] into the [[organizationReport]]. */
    def mergeWith(report: RepositoryReport): OrganizationReport =
      (organizationReport._1.aggregatedTo(report), organizationReport._2 + report)

  extension (m: Map[String, Long])
    /** Aggregates the [[Map]] of contributions with this [[report]]. */
    def aggregatedTo(report: RepositoryReport): Map[String, Long] =
      m ++ report.contributions.map(c => c.user -> (m.getOrElse(c.user, 0L) + c.contributions))
