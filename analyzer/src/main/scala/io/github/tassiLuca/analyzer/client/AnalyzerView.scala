package io.github.tassiLuca.analyzer.client

import io.github.tassiLuca.analyzer.client.view.AnalyzerGUI

trait AnalyzerView:
  def run(): Unit
  def update(result: OrganizationReport): Unit

object AnalyzerView:
  def gui(controller: AppController): AnalyzerView = AnalyzerGUI(controller)
