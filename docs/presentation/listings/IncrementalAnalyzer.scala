  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]] = either:
    Async.group:
      val reposInfo = repositoryService.incrementalRepositoriesOf(organizationName)
      var futureResults = Seq[Future[RepositoryReport]]()
      reposInfo.foreach: repository => // 1
        futureResults = futureResults :+ Future: // 2
          val report = repository.?.performAnalysis.start().awaitResult.?
          synchronized(updateResults(report)) // 3 !
          report
      futureResults.awaitAll // 4
