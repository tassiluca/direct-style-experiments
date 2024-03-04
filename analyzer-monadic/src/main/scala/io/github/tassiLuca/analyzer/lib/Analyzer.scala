package io.github.tassiLuca.analyzer.lib

import cats.data.EitherT
import io.github.tassiLuca.analyzer.commons.lib
import io.github.tassiLuca.analyzer.commons.lib.{Repository, RepositoryReport}
import monix.eval.Task

/** A generic analyzer of organization/group/workspace repositories. */
trait Analyzer:

  /** @return a [[EitherT]] encapsulating a [[Task]] that performs the analysis of the
    * [[organizationName]]'s repositories, providing the results incrementally to the
    * [[updateResults]] function.
    */
  def analyze(organizationName: String)(
      updateResult: RepositoryReport => Unit,
  ): EitherT[Task, String, Seq[RepositoryReport]]

object Analyzer:
  def apply(repositoryService: RepositoryService): Analyzer = AnalyzerImpl(repositoryService)

  private class AnalyzerImpl(repositoryService: RepositoryService) extends Analyzer:

    import cats.implicits.catsSyntaxParallelTraverse1

    override def analyze(organizationName: String)(
        updateResult: RepositoryReport => Unit,
    ): EitherT[Task, String, Seq[RepositoryReport]] =
      for
        repositories <- repositoryService.repositoriesOf(organizationName)
        reports <- repositories.parTraverse(r => EitherT.right(r.performAnalysis(updateResult)))
      yield reports

    extension (r: Repository)
      private def performAnalysis(updateResult: RepositoryReport => Unit): Task[RepositoryReport] =
        val contributorsTask = repositoryService.contributorsOf(r.organization, r.name).value
        val releaseTask = repositoryService.lastReleaseOf(r.organization, r.name).value
        for
          result <- Task.parZip2(contributorsTask, releaseTask)
          report = RepositoryReport(r.name, r.issues, r.stars, result._1.getOrElse(Seq.empty), result._2.toOption)
          _ <- Task(updateResult(report))
        yield report
