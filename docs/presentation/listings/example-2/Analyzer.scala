/** A generic analyzer of organization/group/workspace repositories. */
trait Analyzer:
  def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations, CanFail): Seq[RepositoryReport]

/** A repository report, i.e. all its significant information. */
case class RepositoryReport(
    name: String,
    issues: Int,
    stars: Int,
    contributions: Seq[Contribution],
    lastRelease: Option[Release],
)