package io.github.tassiLuca.analyzer.core

import gears.async.Async
import gears.async.default.given
import io.github.tassiLuca.analyzer.lib.GitHubService
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class GitHubServiceTest extends AnyFunSpec with Matchers {

  private val defaultNumberOfResultsPerPage = 30
  private val gitHubService: GitHubService = GitHubService()
  private val organization = "lampepfl"
  private val repository = "dotty"
  private val odersky = "odersky"

  describe("The GitHubService") {
    describe("when asked for repositories") {
      it("of an existing organization should return all of them") {
        Async.blocking:
          val result = gitHubService.repositoriesOf(organization)
          result.isRight shouldBe true
          result.foreach { repos =>
            repos.size should be > defaultNumberOfResultsPerPage
            repos.foreach(_.organization shouldBe organization)
            repos.count(_.name == repository) shouldBe 1
          }
      }

      it("of a non-existing organization should fail") {
        Async.blocking:
          val nonExistingOrganization = "4315950311"
          val result = gitHubService.repositoriesOf(nonExistingOrganization)
          result.isLeft shouldBe true
      }
    }

    describe("when asked for contributors of an existing repository") {
      it("should return all of them") {
        Async.blocking:
          val result = gitHubService.contributorsOf(organization, repository)
          result.isRight shouldBe true
          result.foreach { contributors =>
            contributors.size should be > defaultNumberOfResultsPerPage
            contributors.exists(_.user == odersky) shouldBe true
          }
      }
    }

    describe("when asked for the last release of an existing repository") {
      it("should return it if it exists") {
        Async.blocking:
          val result = gitHubService.lastReleaseOf(organization, repository)
          result.isRight shouldBe true
      }

      it("should fail if it does not exist") {
        Async.blocking:
          val result = gitHubService.lastReleaseOf(organization, "dotty-website")
          result.isLeft shouldBe true
      }
    }
  }
}
