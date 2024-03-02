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

    private fun CoroutineScope.analyzeAll(organizationName: String, repositories: List<Repository>) =
        Channel<RepositoryReport>().also {
            repositories.map { r ->
                launch {
                    val contributors = async { provider.contributorsOf(organizationName, r.name).getOrThrow() }
                    val release = provider.lastReleaseOf(organizationName, r.name).getOrThrow()
                    it.send(RepositoryReport(r.name, r.issues, r.stars, contributors.await(), release))
                }
            }
        }

    private suspend fun collectResults(
        resultsChannel: Channel<RepositoryReport>,
        expectedResults: Int,
        updateResults: suspend (RepositoryReport) -> Unit,
    ) = mutableSetOf<RepositoryReport>().apply {
        repeat(expectedResults) {
            val report = resultsChannel.receive()
            add(report)
            updateResults(report)
        }
        resultsChannel.close()
    }
}
