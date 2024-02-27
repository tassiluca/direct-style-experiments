package io.github.tassiLuca.analyzer.lib

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A GitHub repository. */
@Serializable
data class Repository(
    /** The ID of the repository. */
    val id: Long,
    /** The full name of the repository, comprising owner organization in `<org>/<repo>` format. */
    @SerialName("full_name") val fullName: String,
    /** The number of stars of the repository. */
    @SerialName("stargazers_count") val stars: Int,
    /** The number of issues of the repository. */
    @SerialName("open_issues_count") val issues: Int,
) {
    /** The name of the organization that owns the repository. */
    val organization: String = fullName.substringBefore('/')

    /** The name of the repository. */
    val name: String = fullName.substringAfter('/')
}

/** A GitHub contribution. */
@Serializable
data class Contribution(
    /** The login name of the contributor. */
    @SerialName("login") val user: String,
    /** The number of contributions. */
    val contributions: Long,
)

/** A GitHub release. */
@Serializable
data class Release(
    /** The tag name of the release. */
    @SerialName("tag_name") val tagName: String,
    /** The published date of the release. */
    @SerialName("published_at") val date: String,
)

/** A report for a GitHub repository. */
data class RepositoryReport(
    /** The name of the repository. */
    val name: String,
    /** The number of open issues. */
    val issues: Int,
    /** The number of stars. */
    val stars: Int,
    /** The number of contributions. */
    val contributions: List<Contribution>,
    /** The last release. */
    val lastRelease: Release?,
)
