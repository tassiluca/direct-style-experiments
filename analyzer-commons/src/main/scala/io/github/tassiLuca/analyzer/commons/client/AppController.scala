package io.github.tassiLuca.analyzer.commons.client

import io.github.tassiLuca.analyzer.commons.lib.RepositoryReport

type OrganizationReport = (Map[String, Long], Set[RepositoryReport])

trait AppController:
  def runSession(organizationName: String): Unit
  def stopSession(): Unit

  extension (organizationReport: OrganizationReport)
    def mergeWith(report: RepositoryReport): OrganizationReport =
      (organizationReport._1.aggregatedTo(report), organizationReport._2 + report)

  extension (m: Map[String, Long])
    def aggregatedTo(report: RepositoryReport): Map[String, Long] =
      m ++ report.contributions.map(c => c.user -> (m.getOrElse(c.user, 0L) + c.contributions))
