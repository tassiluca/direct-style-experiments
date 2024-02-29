package io.github.tassiLuca.analyzer.lib

import gears.async.Async
import gears.async.default.given
import io.github.tassiLuca.analyzer.commons.lib.Repository
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import io.github.tassiLuca.pimping.TerminableChannelOps.toSeq

class GitHubServiceTest extends AnyFunSpec with Matchers {

  private val defaultNumberOfResultsPerPage = 30
  private val gitHubService: RepositoryService = RepositoryService.ofGitHub()
  private val nonExistingOrganization = "4315950311"
  private val organization = "lampepfl"
  private val repository = "dotty"
  private val odersky = "odersky"

  describe("The GitHubService") {
    describe("with paginated blocking behavior") {
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
              contributors.count(_.user == odersky) shouldBe 1
            }
        }
      }
    }

    describe("with paginated incremental results") {
      describe("when asked for repositories") {
        it("of an existing organization should return all of them") {
          Async.blocking:
            val results = gitHubService.incrementalRepositoriesOf(organization).toSeq
            results.size should be > defaultNumberOfResultsPerPage
            results.foreach { r =>
              r.isRight shouldBe true
              r.toOption.get.organization shouldBe organization
            }
            results.map(_.toOption.get.name).count(_ == repository) shouldBe 1
        }

        it("of a non-existing organization should fail") {
          Async.blocking:
            val result = gitHubService.incrementalRepositoriesOf(nonExistingOrganization).toSeq
            result.size shouldBe 1
            result.head.isLeft shouldBe true
        }
      }

      describe("when asked for contributors of an existing repository") {
        it("should return all of them") {
          Async.blocking:
            val results = gitHubService.incrementalContributorsOf(organization, repository).toSeq
            results.size should be > defaultNumberOfResultsPerPage
            results.foreach(_.isRight shouldBe true)
            results.map(_.toOption.get.user).count(_ == odersky) shouldBe 1
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
