package io.github.tassiLuca.analyzer.core

import cats.data.EitherT

import scala.concurrent.{Await, ExecutionContext, Future}

trait Analyzer:
  def analyze(organizationName: String)(
      updateResult: RepositoryReport => Unit,
  )(using ExecutionContext): Future[Either[String, Seq[RepositoryReport]]]

  def analyze2(organizationName: String)(
      updateResult: RepositoryReport => Unit,
  )(using ExecutionContext): EitherT[Future, String, Seq[RepositoryReport]]

object Analyzer:
  def ofGitHub(): Analyzer = new AnalyzerImpl()

  private class AnalyzerImpl extends Analyzer:
    private val gitHubService = GitHubService()

    override def analyze(organizationName: String)(
        updateResult: RepositoryReport => Unit,
    )(using ExecutionContext): Future[Either[String, Seq[RepositoryReport]]] =
      gitHubService.repositoriesOf(organizationName).flatMap {
        case Left(error) => Future.successful(Left(error))
        case Right(repos) =>
          val futuresReports: Seq[Future[RepositoryReport]] = repos.map(performAnalysis)
          val futureSeqOfReports: Future[Seq[RepositoryReport]] = Future.sequence(futuresReports)
          futureSeqOfReports.map(Right(_))
      }

    override def analyze2(organizationName: String)(
        updateResult: RepositoryReport => Unit,
    )(using ExecutionContext): EitherT[Future, String, Seq[RepositoryReport]] =
      import cats.implicits.toTraverseOps
      for
        repositories <- EitherT(gitHubService.repositoriesOf(organizationName))
        reports <- repositories.traverse(r => EitherT.right(r.idiomaticAnalysis))
      yield reports

    extension (r: Repository)
      private def performAnalysis(using ExecutionContext): Future[RepositoryReport] =
        val contributionsTask = gitHubService.contributorsOf(r.organization, r.name)
        val releaseTask = gitHubService.lastReleaseOf(r.organization, r.name)
        for
          contributions <- contributionsTask
          lastRelease <- releaseTask
        yield RepositoryReport(r.name, r.issues, r.stars, contributions.getOrElse(Seq.empty), lastRelease.toOption)

      private def idiomaticAnalysis(using ExecutionContext): Future[RepositoryReport] =
        import cats.implicits.catsSyntaxTuple2Semigroupal
        (gitHubService.contributorsOf(r.organization, r.name), gitHubService.lastReleaseOf(r.organization, r.name))
          .mapN { case (contributions, lastRelease) =>
            RepositoryReport(r.name, r.issues, r.stars, contributions.getOrElse(Seq.empty), lastRelease.toOption)
          }

@main def testAnalyzer(): Unit =
  given ExecutionContext = ExecutionContext.global
  val result = Analyzer.ofGitHub().analyze("unibo-spe")(report => println(report))
  Await.ready(result, scala.concurrent.duration.Duration.Inf)
  println(s">> $result")

@main def testAnalyzerWithCats(): Unit =
  given ExecutionContext = ExecutionContext.global
  val result = Analyzer.ofGitHub().analyze2("unibo-spe")(report => println(report)).value
  Await.ready(result, scala.concurrent.duration.Duration.Inf)
  println(s">> $result")
