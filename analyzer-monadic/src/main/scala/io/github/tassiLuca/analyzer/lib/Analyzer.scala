package io.github.tassiLuca.analyzer.lib

import cats.data.EitherT
import cats.implicits.toTraverseOps
import io.github.tassiLuca.analyzer.commons.lib
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import monix.eval.Task

trait Analyzer:
  def analyze(organizationName: String)(
      updateResult: RepositoryReport => Unit,
  ): EitherT[Task, String, Seq[RepositoryReport]]

object Analyzer:
  def ofGitHub(): Analyzer = AnalyzerImpl()

  private class AnalyzerImpl extends Analyzer:
    private val gitHubService = GitHubService()

    override def analyze(organizationName: String)(
        updateResult: RepositoryReport => Unit,
    ): EitherT[Task, String, Seq[RepositoryReport]] =
      for
        repositories <- gitHubService.repositoriesOf(organizationName)
        reports <- repositories.traverse(r => EitherT.right(r.performAnalysis(updateResult)))
      yield reports

    extension (r: Repository)
      private def performAnalysis(updateResult: RepositoryReport => Unit): Task[RepositoryReport] =
        val contributorsTask = gitHubService.contributorsOf(r.organization, r.name).value
        val releaseTask = gitHubService.lastReleaseOf(r.organization, r.name).value
        for
          result <- Task.parZip2(contributorsTask, releaseTask)
          report = RepositoryReport(r.name, r.issues, r.stars, result._1.getOrElse(Seq.empty), result._2.toOption)
          _ <- Task(updateResult(report))
        yield report
