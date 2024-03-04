package io.github.tassiLuca.analyzer.commons.lib

import upickle.default.ReadWriter

/** A repository contribution. */
case class Contribution(
    @upickle.implicits.key("login") user: String,
    contributions: Long,
) derives ReadWriter

/** A repository. */
case class Repository(
    id: Long,
    @upickle.implicits.key("full_name") fullName: String,
    @upickle.implicits.key("stargazers_count") stars: Int,
    @upickle.implicits.key("open_issues_count") issues: Int,
) derives ReadWriter:
  def organization: String = fullName.split("/")(0)
  def name: String = fullName.split("/")(1)

/** A release, with its info. */
case class Release(
    @upickle.implicits.key("tag_name") tag: String,
    @upickle.implicits.key("published_at") date: String,
) derives ReadWriter:
  override def toString: String = s"$tag @ $date"

/** A repository report, i.e. all its significant information. */
case class RepositoryReport(
    name: String,
    issues: Int,
    stars: Int,
    contributions: Seq[Contribution],
    lastRelease: Option[Release],
)
