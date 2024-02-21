package io.github.tassiLuca.analyzer.core

import gears.async.default.given
import gears.async.Async
import io.github.tassiLuca.analyzer.commons.lib.{Contribution, Release, Repository, RepositoryReport}
import io.github.tassiLuca.analyzer.lib.{Analyzer, GitHubService}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AnalyzerTest extends AnyFlatSpec with Matchers {

  val mockGitHubService: GitHubService = new GitHubService:
    override def repositoriesOf(organizationName: String)(using Async): Either[String, Seq[Repository]] =
      Right(Seq(Repository(0, "dse/test-1", 100, 10), Repository(1, "dse/test-2", 123, 198)))

    override def contributorsOf(
        organizationName: String,
        repositoryName: String,
    )(using Async): Either[String, Seq[Contribution]] = repositoryName match
      case "test-1" => Right(Seq(Contribution("mrossi", 56), Contribution("lpluto", 11)))
      case "test-2" => Right(Seq(Contribution("mrossi", 11), Contribution("averdi", 98)))

    override def lastReleaseOf(organizationName: String, repositoryName: String)(using Async): Either[String, Release] =
      repositoryName match
        case "test-1" => Right(Release("v0.1", "2024-02-21"))
        case _ => Left("404, not found")

  val analyzer: Analyzer = Analyzer.of(mockGitHubService)

  val expectedResults = Seq(
    RepositoryReport(
      "test-1",
      10,
      100,
      Seq(Contribution("mrossi", 56), Contribution("lpluto", 11)),
      Some(Release("v0.1", "2024-02-21")),
    ),
    RepositoryReport(
      "test-2",
      198,
      123,
      Seq(Contribution("mrossi", 11), Contribution("averdi", 98)),
      None,
    ),
  )

  "simple test" should "work" in {
    var results: Set[RepositoryReport] = Set()
    Async.blocking:
      val allResults = analyzer.analyze("dse") { report =>
        results += report
      }
      println(allResults)
      allResults.isRight shouldBe true
      allResults.map(_ should contain theSameElementsAs expectedResults)
      results.toSeq should contain theSameElementsAs expectedResults
  }
}
