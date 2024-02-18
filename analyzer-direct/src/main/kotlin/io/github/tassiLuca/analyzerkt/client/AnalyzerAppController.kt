package io.github.tassiLuca.analyzerkt.client

import io.github.tassiLuca.analyzer.commons.client.AppController
import io.github.tassiLuca.analyzerkt.lib.Analyzer
import io.github.tassiLuca.analyzerkt.lib.RepositoryReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** The analyzer application controller. */
class AnalyzerAppController : AppController, CoroutineScope {
    private val view = AnalyzerGUI(this)
    private val analyzer = Analyzer.ofGitHub()
    private var currentComputation: Job? = null

    init {
        view.run()
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    @Suppress("InjectDispatcher")
    override fun runSession(organizationName: String) {
        var result = Pair(mapOf<String, Long>(), emptySet<RepositoryReport>())
        launch(Dispatchers.Default) {
            analyzer.analyze(organizationName) { report ->
                withContext(Dispatchers.Main) {
                    result = result.first.aggregatedTo(report) to (result.second + report)
                    view.update(result)
                }
            }.onFailure {
                view.error(it.toString())
            }.onSuccess { view.endComputation() }
        }.also { currentComputation = it }
    }

    private fun Map<String, Long>.aggregatedTo(report: RepositoryReport): Map<String, Long> =
        this + report.contributions.map { it.user to ((this[it.user] ?: 0L) + it.contributions) }

    override fun stopSession() {
        currentComputation?.cancel()
        view.cancelled()
    }
}
