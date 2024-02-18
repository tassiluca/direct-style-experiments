package io.github.tassiLuca.analyzerkt.lib

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
        fun ofGitHub(): Analyzer = GitHubAnalyzer()
    }
}

private class GitHubAnalyzer : Analyzer {

    private val gitHubProvider = GitHubRepositoryProvider(GitHubService.create())

    override suspend fun analyze(
        organizationName: String,
        updateResults: suspend (RepositoryReport) -> Unit,
    ): Result<Set<RepositoryReport>> = coroutineScope {
        runCatching {
            val channel = Channel<RepositoryReport>()
            val repositories = gitHubProvider.repositoriesOf(organizationName).getOrThrow()
            repositories.forEach {
                launch {
                    val contributors = async { gitHubProvider.contributorsOf(organizationName, it.name).getOrThrow() }
                    val release = async { gitHubProvider.lastReleaseOf(organizationName, it.name).getOrThrow() }
                    channel.send(RepositoryReport(it.name, it.issues, it.stars, contributors.await(), release.await()))
                }
            }
            var allReports = emptySet<RepositoryReport>()
            repeat(repositories.size) {
                val report = channel.receive()
                allReports = allReports + report
                updateResults(report)
            }
            channel.close()
            allReports
        }
    }
}
