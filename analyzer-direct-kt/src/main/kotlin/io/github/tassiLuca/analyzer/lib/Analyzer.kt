package io.github.tassiLuca.analyzer.lib

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
        /** Creates a new GitHub organization [Analyzer] based on Coroutines `Channel`s. */
        fun ofGitHubByChannels(gitHubProvider: GitHubRepositoryProvider): Analyzer =
            GitHubAnalyzerByChannels(gitHubProvider)

        /** Creates a new GitHub organization [Analyzer] based on Coroutines `Flow`s. */
        fun ofGithubByFlows(gitHubProvider: GitHubRepositoryProvider): Analyzer =
            GitHubAnalyzerByFlows(gitHubProvider)
    }
}
