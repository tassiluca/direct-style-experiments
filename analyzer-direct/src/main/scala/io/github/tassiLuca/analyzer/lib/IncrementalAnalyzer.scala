package io.github.tassiLuca.analyzer.lib

import gears.async.{Async, Future, Listener}
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import io.github.tassiLuca.boundaries.EitherConversions.given
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.?
import io.github.tassiLuca.pimping.ListenerConversions.given
import io.github.tassiLuca.pimping.TerminableChannel
import io.github.tassiLuca.pimping.TerminableChannelOps.foreach

import scala.util.Try

private class IncrementalAnalyzer(repositoryService: RepositoryService) extends Analyzer:

  override def analyze(organizationName: String)(
      updateResults: RepositoryReport => Unit,
  )(using Async): Either[String, Seq[RepositoryReport]] = either:
    val reposInfo = repositoryService.incrementalRepositoriesOf(organizationName)
    var allReports = Seq[RepositoryReport]()
    var fs = Seq[Future[Unit]]()
    reposInfo.foreach { repository =>
      val f = Future:
        val report = repository.?.performAnalysis.awaitResult.?
        updateResults(report)
        allReports = allReports :+ report
      fs = fs :+ f
    }
    fs.awaitAllOrCancel
    allReports

  extension (r: Repository)
    private def performAnalysis(using Async): Future[RepositoryReport] = Future:
      val contributions = Future { repositoryService.contributorsOf(r.organization, r.name) }
      val release = repositoryService.lastReleaseOf(r.organization, r.name)
      RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.toOption)
