package io.github.tassiLuca.analyzer.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class GitHubAnalyzerByChannels(private val provider: GitHubRepositoryProvider) : Analyzer {

    override suspend fun analyze(
        organizationName: String,
        updateResults: suspend (RepositoryReport) -> Unit,
    ): Result<Set<RepositoryReport>> = coroutineScope {
        runCatching {
            val repositories = provider.repositoriesOf(organizationName).getOrThrow()
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
                val contributors = async { provider.contributorsOf(organizationName, it.name).getOrThrow() }
                val release = provider.lastReleaseOf(organizationName, it.name).getOrThrow()
                channel.send(RepositoryReport(it.name, it.issues, it.stars, contributors.await(), release))
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
