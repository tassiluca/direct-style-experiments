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
    /** The number of repository stars. */
    @SerialName("stargazers_count") val stars: Int,
    /** The number of repository issues. */
    @SerialName("open_issues_count") val issues: Int,
) {
    /** The name of the owner organization. */
    val organization: String = fullName.substringBefore('/')

    /** The name of the repository. */
    val name: String = fullName.substringAfter('/')
}

/** A repository contribution. */
@Serializable
data class Contribution(
    /** The login name of the contributor. */
    @SerialName("login") val user: String,
    /** The number of contributions. */
    val contributions: Long,
)

/** A release, with its info. */
@Serializable
data class Release(
    /** The tag name of the release. */
    @SerialName("tag_name") val tagName: String,
    /** The published date of the release. */
    @SerialName("published_at") val date: String,
)

/** A repository report, i.e. all its significant information. */
data class RepositoryReport(
    /** The name of the repository. */
    val name: String,
    /** The issues number. */
    val issues: Int,
    /** The stars number. */
    val stars: Int,
    /** The number of contributions. */
    val contributions: List<Contribution>,
    /** The last release of the repository. */
    val lastRelease: Release?,
)

internal fun Set<RepositoryReport>.addOrUpdate(other: RepositoryReport): Set<RepositoryReport> {
    val updatedSet = this.toMutableSet()
    val existingReport = updatedSet.find { it.name == other.name }
    if (existingReport != null) {
        val mergedContributions = (existingReport.contributions + other.contributions).distinct()
        val updatedReport = existingReport.copy(contributions = mergedContributions)
        updatedSet.remove(existingReport)
        updatedSet.add(updatedReport)
    } else {
        updatedSet.add(other)
    }
    return updatedSet
}
