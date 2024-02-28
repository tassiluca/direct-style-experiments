package io.github.tassiLuca.analyzer.lib

import eu.monniot.scala3mock.ScalaMocks.*
import eu.monniot.scala3mock.scalatest.MockFactory
import gears.async.Async
import gears.async.default.given
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository, RepositoryReport}
import io.github.tassiLuca.analyzer.lib.{Analyzer, RepositoryService}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BasicAnalyzerTest extends AnyFlatSpec with Matchers with MockFactory {

  private val dummiesData = Map[Repository, (Seq[Contribution], Option[Release])](
    Repository(0, "dse/test-1", 100, 10) -> (Seq(Contribution("mrossi", 56)), Some(Release("v0.1", "2024-02-21"))),
    Repository(1, "dse/test-2", 123, 198) -> (Seq(Contribution("mrossi", 11), Contribution("averdi", 98)), None),
  )

  "Analyzer" should "return the correct results if given in input an existing organization" in {
    var incrementalResults = Set[RepositoryReport]()
    Async.blocking:
      val analyzer = successfulService()
      val allResults = analyzer.analyze("dse") { report =>
        incrementalResults += report
      }
      incrementalResults shouldBe expectedResults
      allResults.isRight shouldBe true
      allResults.foreach(_ should contain theSameElementsAs expectedResults)
  }

  "Analyzer" should "return a failure in case the given organization doesn't exists" in {
    var incrementalResults = Set[RepositoryReport]()
    Async.blocking:
      val analyzer = failingService()
      val allResults = analyzer.analyze("non-existing") { report =>
        incrementalResults += report
      }
      incrementalResults shouldBe empty
      allResults.isLeft shouldBe true
  }

  private def expectedResults: Set[RepositoryReport] = dummiesData.collect { case (repo, data) =>
    RepositoryReport(repo.name, repo.issues, repo.stars, data._1, data._2)
  }.toSet

  private def successfulService(): Analyzer =
    val gitHubService: RepositoryService = mock[RepositoryService]
    when(gitHubService.repositoriesOf(_: String)(using _: Async)).expects("dse", *)
      .returning(Right(dummiesData.keys.toSeq))
    dummiesData.foreach { (repo, data) =>
      when(gitHubService.contributorsOf(_: String, _: String)(using _: Async)).expects(repo.organization, repo.name, *)
        .returning(Right(data._1))
      when(gitHubService.lastReleaseOf(_: String, _: String)(using _: Async)).expects(repo.organization, repo.name, *)
        .returning(data._2.toRight("404, not found"))
    }
    Analyzer.basic(gitHubService)

  private def failingService(): Analyzer =
    val gitHubService: RepositoryService = mock[RepositoryService]
    when(gitHubService.repositoriesOf(_: String)(using _: Async)).expects("non-existing", *)
      .returning(Left("404, not found"))
    Analyzer.basic(gitHubService)
}
