package io.github.tassiLuca.analyzer.lib

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking

class GitHubRepositoryProviderTest : FreeSpec() {

    private val gitHubRepositoryProvider = GitHubRepositoryProvider(GitHubService.create())

    init {
        "The github repository provider" - {
            "when asked for repositories" - {
                "of an existing organization should return them" - {
                    runBlocking {
                        val result = gitHubRepositoryProvider.repositoriesOf(ORGANIZATION)
                        result.isSuccess shouldBe true
                        val checkedResult = result.getOrThrow()
                        checkedResult.size shouldBeGreaterThan DEFAULT_NUMBER_OF_RESULTS_PER_PAGE
                        checkedResult.forEach { it.organization shouldBe ORGANIZATION }
                        checkedResult.map { it.name } shouldContain REPOSITORY
                    }
                }

                "of a non-existing organization should return a failure" - {
                    runBlocking {
                        val nonExistingOrganization = "4315950311"
                        val result = gitHubRepositoryProvider.repositoriesOf(nonExistingOrganization)
                        result.isFailure shouldBe true
                    }
                }
            }

            "when asked for contributors of an existing repository" - {
                "should return all of them" - {
                    runBlocking {
                        val result = gitHubRepositoryProvider.contributorsOf(ORGANIZATION, REPOSITORY)
                        result.isSuccess shouldBe true
                        val checkedResult = result.getOrThrow()
                        checkedResult.size shouldBeGreaterThan DEFAULT_NUMBER_OF_RESULTS_PER_PAGE
                        checkedResult.map { it.user } shouldContain ODERSKY
                    }
                }
            }

            "when asked for the last release of an existing repository" - {
                "should return it if it exists" - {
                    runBlocking {
                        val result = gitHubRepositoryProvider.lastReleaseOf(ORGANIZATION, REPOSITORY)
                        result.isSuccess shouldBe true
                    }
                }

                "should return a failure if it doesn't exists" - {
                    runBlocking {
                        val result = gitHubRepositoryProvider.lastReleaseOf(ORGANIZATION, "dotty-website")
                        result.isSuccess shouldBe true
                        result.getOrThrow() shouldBe null
                    }
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_NUMBER_OF_RESULTS_PER_PAGE = 30
        private const val ORGANIZATION = "lampepfl"
        private const val REPOSITORY = "dotty"
        private const val ODERSKY = "odersky"
    }
}
