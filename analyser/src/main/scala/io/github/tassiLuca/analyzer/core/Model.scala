package io.github.tassiLuca.analyzer.core

import upickle.default.ReadWriter

case class Contribution(
    @upickle.implicits.key("login") user: String,
    contributions: Long,
) derives ReadWriter

case class Repository(
    id: Long,
    name: String,
    @upickle.implicits.key("stargazers_count") stars: Int,
    @upickle.implicits.key("open_issues_count") issues: Int,
) derives ReadWriter

case class Release(
    @upickle.implicits.key("tag_name") tag: String,
    @upickle.implicits.key("published_at") date: String,
) derives ReadWriter

case class RepositoryReport(
    name: String,
    issues: Int,
    stars: Int,
    contributions: Set[Contribution],
    lastRelease: Option[Release],
)
