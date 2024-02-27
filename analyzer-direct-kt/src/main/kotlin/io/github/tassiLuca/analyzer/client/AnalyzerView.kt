package io.github.tassiLuca.analyzer.client

import io.github.tassiLuca.analyzer.lib.RepositoryReport

typealias OrganizationReport = Pair<Map<String, Long>, Set<RepositoryReport>>

/** A view for the analyzer application. */
interface AnalyzerView {

    /** Runs the view. */
    fun run()

    /** Updates the view with the given [result]. */
    fun update(result: OrganizationReport)

    /** Notify the view the end of the computation. */
    fun endComputation()

    /** Display the given [errorMessage]. */
    fun error(errorMessage: String)

    /** Notify the view the computation has been cancelled. */
    fun cancelled()
}
