package io.github.tassiLuca.analyzer.commons.client

/** The view of the analyzer application. */
trait AnalyzerView:

  /** Start the view. */
  def run(): Unit

  /** Update the view with the [[result]] of the computation. */
  def update(result: OrganizationReport): Unit

  /** Notify the end of the computation. */
  def endComputation(): Unit

  /** Notify an [[errorMessage]]. */
  def error(errorMessage: String): Unit

  /** Notify the cancellation of the computation. */
  def cancelled(): Unit

object AnalyzerView:

  /** A graphical analyzer view. */
  def gui(controller: AppController): AnalyzerView = AnalyzerGUI(controller)
