package io.github.tassiLuca.analyzer.lib

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AnalyzerTest : FunSpec() {

    private val dummiesData: Map<Repository, Pair<Set<Contribution>, Release?>> = mapOf(
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
                allResults.getOrThrow() shouldContainAll expectedResults()
                incrementalResults shouldContainAll expectedResults()
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

    private suspend fun successfulService(): Analyzer {
        val gitHubProvider = mock<GitHubRepositoryProvider>()
        `when`(gitHubProvider.repositoriesOf("dse"))
            .thenReturn(Result.success(dummiesData.keys.toList()))
        dummiesData.forEach { (repo, data) ->
            `when`(gitHubProvider.contributorsOf(repo.organization, repo.name))
                .thenReturn(Result.success(data.first.toList()))
            `when`(gitHubProvider.lastReleaseOf(repo.organization, repo.name))
                .thenReturn(Result.success(data.second))
        }
        return Analyzer.ofGitHubByChannels(gitHubProvider)
    }

    private suspend fun failingService(): Analyzer {
        val gitHubProvider = mock<GitHubRepositoryProvider>()
        `when`(gitHubProvider.repositoriesOf("dse"))
            .thenReturn(Result.failure(RuntimeException("404, not found")))
        return Analyzer.ofGitHubByChannels(gitHubProvider)
    }
}
