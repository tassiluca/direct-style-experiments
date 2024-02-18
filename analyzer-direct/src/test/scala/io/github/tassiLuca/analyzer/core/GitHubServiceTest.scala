package io.github.tassiLuca.analyzer.core

import gears.async.Async
import gears.async.default.given
import io.github.tassiLuca.analyzer.lib.GitHubService
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class GitHubServiceTest extends AnyFunSpec with Matchers {

  val DEFAULT_NUMBER_OF_RESULTS_PER_PAGE = 30
  val gitHubService: GitHubService = GitHubService()
  val organization = "lampepfl"
  val repository = "dotty"
  val odersky = "odersky"

  describe("GitHubService") {
    describe("repositoriesOf") {
      it("should return all the repositories of a given organization") {
        Async.blocking:
          val result = gitHubService.repositoriesOf(organization)
          result.isRight shouldBe true
          result.foreach { repos =>
            repos.size should be > DEFAULT_NUMBER_OF_RESULTS_PER_PAGE
            repos.foreach(_.organization shouldBe organization)
            repos.count(_.name == repository) shouldBe 1
          }
      }

      it("should return a error message if the organization doesn't exist") {
        Async.blocking:
          val result = gitHubService.repositoriesOf("4315950311")
          result.isLeft shouldBe true
      }
    }

    describe("contributorsOf") {
      it("should return all the contributors of a given repository") {
        Async.blocking:
          val result = gitHubService.contributorsOf(organization, repository)
          result.isRight shouldBe true
          result.foreach { contributors =>
            contributors.size should be > DEFAULT_NUMBER_OF_RESULTS_PER_PAGE
            println(contributors.size)
            contributors.exists(_.user == odersky) shouldBe true
          }
      }
    }

    describe("lastReleaseOf") {
      it("should return the last release of a repository if it exists") {
        Async.blocking:
          val result = gitHubService.lastReleaseOf(organization, repository)
          result.isRight shouldBe true
      }

      it("should return a error message if it does not exist") {
        Async.blocking:
          val result = gitHubService.lastReleaseOf(organization, "dotty-website")
          result.isLeft shouldBe true
      }
    }
  }
}
