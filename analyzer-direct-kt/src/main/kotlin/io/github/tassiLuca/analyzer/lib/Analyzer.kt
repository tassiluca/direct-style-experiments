package io.github.tassiLuca.analyzer.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** A generic analyzer of organization/group/workspace repositories. */
interface Analyzer {

    /**
     * Performs a **suspending** analysis of the [organizationName]'s repositories, providing the results
     * incrementally to the [updateResults] function, along with the indication of the completion.
     */
    suspend fun analyze(
        organizationName: String,
        updateResults: suspend (RepositoryReport) -> Unit = { _ -> },
    ): Result<Set<RepositoryReport>>

    companion object {
        /** Creates a new GitHub organization [Analyzer]. */
        fun ofGitHub(gitHubProvider: GitHubRepositoryProvider): Analyzer = GitHubAnalyzer(gitHubProvider)
    }
}

private class GitHubAnalyzer(private val gitHubProvider: GitHubRepositoryProvider) : Analyzer {

    override suspend fun analyze(
        organizationName: String,
        updateResults: suspend (RepositoryReport) -> Unit,
    ): Result<Set<RepositoryReport>> = coroutineScope {
        runCatching {
            val repositories = gitHubProvider.repositoriesOf(organizationName).getOrThrow()
            val resultsChannel = analyzeAll(organizationName, repositories)
            collectResults(resultsChannel, repositories.size, updateResults)
        }
    }

    private fun CoroutineScope.analyzeAll(
        organizationName: String,
        repositories: List<Repository>,
    ): Channel<RepositoryReport> {
        val channel = Channel<RepositoryReport>()
        repositories.map {
            launch {
                val contributors = async { gitHubProvider.contributorsOf(organizationName, it.name).getOrThrow() }
                val release = async { gitHubProvider.lastReleaseOf(organizationName, it.name).getOrThrow() }
                channel.send(RepositoryReport(it.name, it.issues, it.stars, contributors.await(), release.await()))
            }
        }
        return channel
    }

    private suspend fun collectResults(
        resultsChannel: Channel<RepositoryReport>,
        expectedResults: Int,
        updateResults: suspend (RepositoryReport) -> Unit,
    ): Set<RepositoryReport> {
        var allReports = emptySet<RepositoryReport>()
        repeat(expectedResults) {
            val report = resultsChannel.receive()
            allReports = allReports + report
            updateResults(report)
        }
        resultsChannel.close()
        return allReports
    }
}
