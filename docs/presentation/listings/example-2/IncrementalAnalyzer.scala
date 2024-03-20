override def analyze(organizationName: String)(
    updateResults: RepositoryReport => Unit,
)(using Async, AsyncOperations, CanFail): Seq[RepositoryReport] = Async.group:
  val reposInfo = repositoryService.incrementalRepositoriesOf(organizationName)
  var futureResults = Seq[Future[RepositoryReport]]()
  reposInfo.foreach: repository =>
    futureResults = futureResults :+ Future:
      val report = repository.?.performAnalysis.start().awaitResult.?
      synchronized(updateResults(report))
      report
  futureResults.awaitAllOrCancel