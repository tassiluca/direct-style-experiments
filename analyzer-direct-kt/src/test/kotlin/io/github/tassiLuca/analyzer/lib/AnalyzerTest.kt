package io.github.tassiLuca.analyzer.lib

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

abstract class AnalyzerTest : FunSpec() {

    protected val dummiesData: Map<Repository, Pair<Set<Contribution>, Release?>> = mapOf(
        Repository(0, "dse/test-1", 100, 10) to
            Pair(setOf(Contribution("mrossi", 56)), Release("v0.1", "2024-02-21")),
        Repository(1, "dse/test-2", 123, 198) to
            Pair(setOf(Contribution("mrossi", 11), Contribution("averdi", 98)), null),
    )

    init {
        test("Channel based analyzer should return the correct results if no errors occur") {
            var incrementalResults = emptySet<RepositoryReport>()
            runBlocking {
                val service = successfulService()
                val allResults = service.analyze("dse") {
                    incrementalResults = incrementalResults + it
                }
                allResults.isSuccess shouldBe true
                incrementalResults.isEmpty() shouldBe false
                allResults.getOrThrow() shouldContainAll expectedResults()
            }
        }

        test("Channel based analyzer should return a failure if given in input a non-existing organization") {
            var incrementalResults = emptySet<RepositoryReport>()
            runBlocking {
                val service = failingService()
                val allResults = service.analyze("dse") {
                    incrementalResults = incrementalResults + it
                }
                allResults.isFailure shouldBe true
                incrementalResults.shouldBeEmpty()
            }
        }
    }

    private fun expectedResults() = dummiesData.map {
        RepositoryReport(it.key.name, it.key.issues, it.key.stars, it.value.first.toList(), it.value.second)
    }.toSet()

    abstract fun analyzer(provider: GitHubRepositoryProvider): Analyzer

    private suspend fun successfulService(): Analyzer {
        val gitHubProvider = mock<GitHubRepositoryProvider>()
        registerSuccessfulRepositoriesResult(gitHubProvider)
        dummiesData.forEach { (repo, data) ->
            `when`(gitHubProvider.lastReleaseOf(repo.organization, repo.name))
                .thenReturn(Result.success(data.second))
        }
        return analyzer(gitHubProvider)
    }

    abstract suspend fun registerSuccessfulRepositoriesResult(provider: GitHubRepositoryProvider)

    private suspend fun failingService(): Analyzer {
        val gitHubProvider = mock<GitHubRepositoryProvider>()
        registerFailingRepositoriesResult(gitHubProvider)
        return analyzer(gitHubProvider)
    }

    abstract suspend fun registerFailingRepositoriesResult(provider: GitHubRepositoryProvider)
}

class GitHubAnalyzerByChannelsTest : AnalyzerTest() {
    override fun analyzer(provider: GitHubRepositoryProvider): Analyzer = Analyzer.ofGitHubByChannels(provider)

    override suspend fun registerSuccessfulRepositoriesResult(provider: GitHubRepositoryProvider) {
        `when`(provider.repositoriesOf("dse"))
            .thenReturn(Result.success(dummiesData.keys.toList()))
        dummiesData.forEach { (repo, data) ->
            `when`(provider.contributorsOf(repo.organization, repo.name))
                .thenReturn(Result.success(data.first.toList()))
        }
    }
    override suspend fun registerFailingRepositoriesResult(provider: GitHubRepositoryProvider) {
        `when`(provider.repositoriesOf("dse"))
            .thenReturn(Result.failure(RuntimeException("404, not found")))
    }
}

class GitHubAnalyzerByFlowsTest : AnalyzerTest() {
    override fun analyzer(provider: GitHubRepositoryProvider): Analyzer = Analyzer.ofGithubByFlows(provider)

    override suspend fun registerSuccessfulRepositoriesResult(provider: GitHubRepositoryProvider) {
        `when`(provider.flowingRepositoriesOf("dse"))
            .thenReturn(dummiesData.keys.asFlow().map { println(it); listOf(it) })
        dummiesData.forEach { (repo, data) ->
            `when`(provider.flowingContributorsOf(repo.organization, repo.name))
                .thenReturn(data.first.asFlow().map { listOf(it) })
        }
    }
    override suspend fun registerFailingRepositoriesResult(provider: GitHubRepositoryProvider) {
        `when`(provider.flowingRepositoriesOf("dse"))
            .thenReturn(flow { throw RuntimeException("404, not found") })
    }
}
