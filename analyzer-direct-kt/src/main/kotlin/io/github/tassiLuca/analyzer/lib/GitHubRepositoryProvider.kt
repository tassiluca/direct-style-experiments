package io.github.tassiLuca.analyzer.lib

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.awaitResponse

/** A facade for the [GitHubService]. */
class GitHubRepositoryProvider(private val gitHubService: GitHubService) {

    /** Returns the repositories of the given [organizationName]. */
    suspend fun repositoriesOf(organizationName: String): Result<List<Repository>> =
        paginatedRequest { gitHubService.organizationRepositories(organizationName, it).awaitResponse() }

    /** Returns the repositories of the given [organizationName] as [Flow]. */
    fun flowingRepositoriesOf(organizationName: String): Flow<List<Repository>> =
        paginatedFlowRequest { gitHubService.organizationRepositories(organizationName, it).awaitResponse() }

    /** Returns the contributors of the given [organizationName] and [repositoryName]. */
    suspend fun contributorsOf(organizationName: String, repositoryName: String): Result<List<Contribution>> =
        paginatedRequest { gitHubService.contributorsOf(organizationName, repositoryName, it).awaitResponse() }

    /** Returns the contributors of the given [organizationName] and [repositoryName] as [Flow]. */
    fun flowingContributorsOf(organizationName: String, repositoryName: String): Flow<List<Contribution>> =
        paginatedFlowRequest { gitHubService.contributorsOf(organizationName, repositoryName, it).awaitResponse() }

    /** Returns the last release of the given [organizationName] and [repositoryName]. */
    suspend fun lastReleaseOf(organizationName: String, repositoryName: String): Result<Release?> =
        gitHubService.lastReleaseOf(organizationName, repositoryName).awaitResponse().let {
            if (!it.isSuccessful && it.code() != NOT_FOUND) {
                Result.failure(HttpException(it))
            } else {
                Result.success(it.body())
            }
        }

    private suspend fun <T> paginatedRequest(requestCall: suspend (Int) -> Response<List<T>>): Result<List<T>> {
        suspend fun withPagination(partialResult: List<T>, next: Int?): Result<List<T>> {
            return when (next) {
                null -> Result.success(partialResult)
                else -> with(requestCall(next)) {
                    if (!isSuccessful) {
                        Result.failure(HttpException(this))
                    } else {
                        withPagination(partialResult + body().orEmpty(), nextPage()?.toInt())
                    }
                }
            }
        }
        return withPagination(emptyList(), 1)
    }

    private fun <T> paginatedFlowRequest(requestCall: suspend (Int) -> Response<List<T>>): Flow<List<T>> {
        suspend fun FlowCollector<List<T>>.withPagination(next: Int?) {
            return when (next) {
                null -> {}
                else -> requestCall(next).let {
                    if (!it.isSuccessful) {
                        throw HttpException(it)
                    } else {
                        emit(it.body().orEmpty())
                        withPagination(it.nextPage()?.toInt())
                    }
                }
            }
        }
        return flow { withPagination(1) }
    }

    private fun <T> Response<List<T>>.nextPage(): String? = headers()
        .values("Link")
        .flatMap { it.split(",") }
        .find { it.contains("rel=\"next\"") }
        ?.let { Regex("""[?&]page=(\d+)""").find(it) }?.groupValues?.get(1)

    companion object {
        private const val NOT_FOUND = 404
    }
}
