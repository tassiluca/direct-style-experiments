class BasicAnalyzer(repositoryService: RepositoryService) extends AbstractAnalyzer(repositoryService):

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async, AsyncOperations): Either[String, Seq[RepositoryReport]] = either:
    Async.group:
      val reposInfo = repositoryService.repositoriesOf(organizationName).? // 1
        .map(_.performAnalysis.start()) // 2
      val collector = Collector[RepositoryReport](reposInfo.toList*) // 3
      reposInfo.foreach: _ => // 4
        updateResults(collector.results.read().asTry.?.awaitResult.?)
      reposInfo.awaitAll // 5