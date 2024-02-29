package io.github.tassiLuca.analyzer.commons.client

import io.github.tassiLuca.analyzer.commons.lib.RepositoryReport

trait AnalyzerView:
  def run(): Unit
  def update(result: OrganizationReport): Unit
  def endComputation(): Unit
  def error(errorMessage: String): Unit
  def cancelled(): Unit

object AnalyzerView:
  def gui(controller: AppController): AnalyzerView = AnalyzerGUI(controller)
