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
    val collector = TerminableChannel.ofUnbounded[Try[RepositoryReport]]
    val f1 = Future:
      var fs = Seq[Future[RepositoryReport]]()
      reposInfo.foreach { repository =>
        val f = repository.?.performAnalysis
        f.onComplete(Listener((r, _) => collector.send(r)))
        fs = fs :+ f
      }
      fs.awaitAllOrCancel
    f1.onComplete(() => collector.terminate())
    val f2 = Future:
      var allReports = Seq[RepositoryReport]()
      collector.foreach(f =>
        val report = f.?
        updateResults(report)
        allReports = allReports :+ report,
      )
      allReports
    f1.zip(f2).await._2

  extension (r: Repository)
    private def performAnalysis(using Async): Future[RepositoryReport] = Future:
      val contributions = Future { repositoryService.contributorsOf(r.organization, r.name) }
      val release = repositoryService.lastReleaseOf(r.organization, r.name)
      RepositoryReport(r.name, r.issues, r.stars, contributions.await.getOrElse(Seq()), release.toOption)
