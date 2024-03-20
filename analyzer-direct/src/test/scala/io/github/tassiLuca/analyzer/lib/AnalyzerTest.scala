package io.github.tassiLuca.analyzer.lib

import gears.async.default.given
import eu.monniot.scala3mock.ScalaMocks.{mock, when}
import eu.monniot.scala3mock.scalatest.MockFactory
import gears.async.Async
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository, RepositoryReport}
import io.github.tassiLuca.dse.boundaries.{CanFail, either}
import io.github.tassiLuca.dse.pimping.TerminableChannel
import org.scalatest.Ignore
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.boundary.break

// TODO: The following test classes are ignored since would require to use mocking
//  with multiple using clauses. See below for more details
abstract class AnalyzerTest extends AnyFlatSpec with Matchers with MockFactory:
  protected val dummiesData = Map[Repository, (Seq[Contribution], Option[Release])](
    Repository(0, "dse/test-1", 100, 10) -> (Seq(Contribution("mrossi", 56)), Some(Release("v0.1", "2024-02-21"))),
    Repository(1, "dse/test-2", 123, 198) -> (Seq(Contribution("mrossi", 11), Contribution("averdi", 98)), None),
  )

  "Analyzer" should "return the correct results if given in input an existing organization" in {
    var incrementalResults = Set[RepositoryReport]()
    Async.blocking:
      either:
        val allResults = successfulService.analyze("dse")(incrementalResults += _)
        incrementalResults shouldBe expectedResults
        allResults should contain theSameElementsAs expectedResults
      .isRight shouldBe true
  }

  "Analyzer" should "return a failure in case the given organization doesn't exists" in {
    var incrementalResults = Set[RepositoryReport]()
    Async.blocking:
      either:
        failingService.analyze("non-existing")(incrementalResults += _)
      .isLeft shouldBe true
      incrementalResults shouldBe empty
  }

  private def expectedResults: Set[RepositoryReport] = dummiesData.collect { case (repo, data) =>
    RepositoryReport(repo.name, repo.issues, repo.stars, data._1, data._2)
  }.toSet

  val analyzerProvider: RepositoryService => Analyzer

  def successfulService(using Async, CanFail): Analyzer =
    val gitHubService: RepositoryService = mock[RepositoryService]
    registerSuccessfulRepositoriesResult(gitHubService)
    dummiesData.foreach: (repo, data) =>
      /* TODO: HERE AND IN SUBSEQUENT MOCKS!
       *  To investigate how to use mock with multiple using clauses...
       *  It seems that it is not yet supported; it is not documented, at least :/
       *                                                      VVVVVVVVVVVVVVV
       */
      when(gitHubService.contributorsOf(_: String, _: String)(using _: Async))
        .expects(repo.organization, repo.name, *)
        .returning(data._1)
      when(gitHubService.lastReleaseOf(_: String, _: String)(using _: Async))
        .expects(repo.organization, repo.name, *)
        .returning(data._2.getOrElse(break(Left("No release found"))))
    analyzerProvider(gitHubService)

  def registerSuccessfulRepositoriesResult(service: RepositoryService)(using Async, CanFail): Any

  def failingService(using Async, CanFail): Analyzer =
    val gitHubService: RepositoryService = mock[RepositoryService]
    registerFailureRepositoriesResult(gitHubService)
    analyzerProvider(gitHubService)

  def registerFailureRepositoriesResult(service: RepositoryService)(using Async, CanFail): Any
end AnalyzerTest

@Ignore
class BasicAnalyzerTest extends AnalyzerTest:

  override val analyzerProvider: RepositoryService => Analyzer = Analyzer.basic

  override def registerSuccessfulRepositoriesResult(service: RepositoryService)(using async: Async, canFail: CanFail) =
    when(service.repositoriesOf(_: String)(using _: Async))
      .expects("dse", *)
      .returning(dummiesData.keys.toSeq)

  override def registerFailureRepositoriesResult(service: RepositoryService)(using Async, CanFail) =
    when(service.repositoriesOf(_: String)(using _: Async))
      .expects("non-existing", *)
      .returning(break(Left("404, not found")))
end BasicAnalyzerTest

@Ignore
class IncrementalAnalyzerTest extends AnalyzerTest:

  override val analyzerProvider: RepositoryService => Analyzer = Analyzer.incremental

  override def registerSuccessfulRepositoriesResult(service: RepositoryService)(using Async, CanFail): Any =
    val repositoriesResult = TerminableChannel.ofUnbounded[Either[String, Repository]]
    dummiesData.keys.foreach(repo => repositoriesResult.send(Right(repo)))
    repositoriesResult.terminate()
    when(service.incrementalRepositoriesOf(_: String)(using _: Async.Spawn))
      .expects("dse", *)
      .returning(repositoriesResult)

  override def registerFailureRepositoriesResult(service: RepositoryService)(using Async, CanFail): Any =
    val repositoriesResult = TerminableChannel.ofUnbounded[Either[String, Repository]]
    repositoriesResult.send(Left("404, not found"))
    repositoriesResult.terminate()
    when(service.incrementalRepositoriesOf(_: String)(using _: Async.Spawn))
      .expects("non-existing", *)
      .returning(repositoriesResult)
end IncrementalAnalyzerTest
