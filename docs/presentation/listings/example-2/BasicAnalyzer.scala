def performAnalysis(using Async): Task[RepositoryReport] = ...

override def analyze(organizationName: String)(
    updateResults: RepositoryReport => Unit,
)(using Async, AsyncOperations, CanFail): Seq[RepositoryReport] = Async.group:
  val reposInfo = repositoryService.repositoriesOf(organizationName)
    .map(_.performAnalysis.start())
  val collector = Collector(reposInfo.toList*)
  reposInfo.foreach: _ =>
    updateResults(collector.results.read().asTry.?.awaitResult.?)
  reposInfo.awaitAll