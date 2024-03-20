/** A generic analyzer of organization/group/workspace repositories. */
trait Analyzer:

  def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations, CanFail): Seq[RepositoryReport]