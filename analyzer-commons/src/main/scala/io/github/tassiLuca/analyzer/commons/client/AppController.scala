package io.github.tassiLuca.analyzer.commons.client

import io.github.tassiLuca.analyzer.commons.lib.RepositoryReport

type OrganizationReport = (Map[String, Long], Set[RepositoryReport])

trait AppController:
  def runSession(organizationName: String): Unit
  def stopSession(): Unit

