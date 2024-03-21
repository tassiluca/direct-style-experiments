Async.blocking:
  val reposFlow = gitHubService.flowingRepositoriesOf("scala")
  log("Still not collecting...")
  sleep(1000)
  log("Starting collecting...")
  reposFlow.collect(log)
  log("Done!")