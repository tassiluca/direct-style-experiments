package io.github.tassiLuca.analyzer.lib

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Service to interact with GitHub API. */
interface GitHubService {

    /** @return the repositories of the organization with the given name. */
    @GET("orgs/{organizationName}/repos")
    fun repositoriesOf(
        @Path("organizationName") organizationName: String,
        @Query("page") page: Int = 1,
    ): Call<List<Repository>>

    /** @return the contributors of the repository with given organization and name. */
    @GET("repos/{organizationName}/{repositoryName}/contributors")
    fun contributorsOf(
        @Path("organizationName") organizationName: String,
        @Path("repositoryName") repositoryName: String,
        @Query("page") page: Int = 1,
    ): Call<List<Contribution>>

    /** @return the last release of the repository with given organization and name. */
    @GET("repos/{organizationName}/{repositoryName}/releases/latest")
    fun lastReleaseOf(
        @Path("organizationName") organizationName: String,
        @Path("repositoryName") repositoryName: String,
    ): Call<Release>

    companion object {
        /** Creates a new [GitHubService] instance. */
        fun create(): GitHubService {
            val authToken = System.getenv("GH_TOKEN")
            val httpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $authToken")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()
                    chain.proceed(request)
                }
                .addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    if (!response.isSuccessful) {
                        val message = "GitHub API request failed with status code ${response.code}."
                        response.newBuilder().message(message).build()
                    } else {
                        response
                    }
                }
                .build()
            val contentType = "application/json".toMediaType()
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
                .client(httpClient)
                .build()
            return retrofit.create(GitHubService::class.java)
        }
    }
}
