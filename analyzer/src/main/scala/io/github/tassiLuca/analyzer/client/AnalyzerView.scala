package io.github.tassiLuca.analyzer.client

trait AnalyzerView:
  def run(): Unit
  def update(result: OrganizationReport): Unit
  def endComputation(): Unit
  def error(errorMessage: String): Unit

object AnalyzerView:
  def gui(controller: AppController): AnalyzerView = AnalyzerGUI(controller)
