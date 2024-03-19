trait Analyzer:

  /** Performs a **suspending** analysis of the [[organizationName]]'s repositories, 
    * providing the results incrementally to the [[updateResults]] function.
    */
  def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]]