package io.github.tassiLuca.analyzer.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

internal class GitHubAnalyzerByFlows(private val provider: GitHubRepositoryProvider) : Analyzer {

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressWarnings("InjectDispatcher")
    override suspend fun analyze(
        organizationName: String,
        updateResults: suspend (RepositoryReport) -> Unit,
    ): Result<Set<RepositoryReport>> = coroutineScope {
        runCatching {
            val reports = provider.flowingRepositoriesOf(organizationName)
                .flatMapConcat { analyzeAll(it) }
                .flowOn(Dispatchers.Default)
            var allReports = emptySet<RepositoryReport>()
            reports.collect {
                updateResults(it)
                allReports = allReports + it
            }
            allReports
        }
    }

    @SuppressWarnings("InjectDispatcher")
    private fun analyzeAll(repositories: List<Repository>): Flow<RepositoryReport> = channelFlow {
        repositories.forEach { repository ->
            launch {
                val release = async { provider.lastReleaseOf(repository.organization, repository.name).getOrThrow() }
                provider.flowingContributorsOf(repository.organization, repository.name).toList().forEach {
                    send(RepositoryReport(repository.name, repository.issues, repository.stars, it, release.await()))
                }
            }
        }
    }.flowOn(Dispatchers.Default)
}
