package io.github.tassiLuca.analyzer.coroutines.core

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

/** A generic analyzer of organization/group/workspace repositories. */
interface Analyzer {

    /**
     * Performs a **suspending** analysis of the [organizationName]'s repositories, providing the results
     * incrementally to the [updateResults] function, along with the indication of the completion.
     */
    suspend fun analyze(
        organizationName: String,
        updateResults: suspend (RepositoryReport, completed: Boolean) -> Unit = { _, _ -> },
    ): Result<Set<RepositoryReport>>

    companion object {
        /** Creates a new GitHub organization [Analyzer]. */
        fun github(): Analyzer = GitHubAnalyzer()
    }
}

private class GitHubAnalyzer : Analyzer {

    private val service = GitHubService.create()

    override suspend fun analyze(
        organizationName: String,
        updateResults: suspend (RepositoryReport, completed: Boolean) -> Unit,
    ): Result<Set<RepositoryReport>> = coroutineScope {
        runCatching {
            val repositories = service
                .organizationRepositories(organizationName)
                .awaitResponse()
                .body() ?: error("No repositories found for the organization $organizationName.")
            val channel = Channel<RepositoryReport>()
            for (repository in repositories) {
                launch {
                    val contributors = service.contributorsOf(organizationName, repository.name)
                        .awaitResponse()
                        .body().orEmpty()
                    val lastRelease = service.lastReleaseOf(organizationName, repository.name)
                        .awaitResponse()
                        .body()
                    with(repository) {
                        channel.send(
                            RepositoryReport(name, issues, stars, contributors, lastRelease),
                        )
                    }
                }
            }
            var allReports = emptySet<RepositoryReport>()
            repeat(repositories.size) {
                val report = channel.receive()
                allReports += report
                updateResults(report, it == repositories.lastIndex)
            }
            allReports
        }
    }
}
