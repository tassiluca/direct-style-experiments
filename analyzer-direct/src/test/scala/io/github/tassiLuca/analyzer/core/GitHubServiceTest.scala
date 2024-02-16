package io.github.tassiLuca.analyzer.core

import gears.async.Async
import gears.async.default.given
import io.github.tassiLuca.analyzer.core.direct.GitHubService
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class GitHubServiceTest extends AnyFunSpec with Matchers {

  val gitHubService: GitHubService = GitHubService()
  val organization = "lampepfl"
  val repository = "gears"
  val odersky = "odersky"

  ignore /*describe*/ ("GitHubService") {
    describe("repositoriesOf") {
      it("should return all the repositories of a given organization") {
        Async.blocking:
          val result = gitHubService.repositoriesOf(organization)
          result.isRight shouldBe true
          result.foreach { repos =>
            repos.size should be > 0
            repos.foreach(_.organization shouldBe organization)
            repos.count(_.name == repository) shouldBe 1
          }
      }

      it("should return an error message if the organization does not exist") {
        Async.blocking:
          val result = gitHubService.repositoriesOf("non-existent-organization")
          result.isLeft shouldBe true
      }
    }

    describe("contributorsOf") {
      it("should return all the contributors of a given repository") {
        Async.blocking:
          val result = gitHubService.contributorsOf(organization, repository)
          result.isRight shouldBe true
          result.foreach { contributors =>
            contributors.size should (be > 0)
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
