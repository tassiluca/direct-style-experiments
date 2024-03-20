package io.github.tassiLuca.analyzer.lib

import gears.async.Async
import gears.async.AsyncOperations.sleep
import gears.async.default.given
import io.github.tassiLuca.analyzer.commons.lib.Repository
import io.github.tassiLuca.dse.boundaries.either
import io.github.tassiLuca.dse.pimping.TerminableChannelOps.toSeq
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class GitHubServiceTest extends AnyFunSpec with Matchers {

  private val defaultNumberOfResultsPerPage = 30
  private val gitHubService: RepositoryService = RepositoryService.ofGitHub()
  private val nonExistingOrganization = "4315950311"
  private val organization = "scala"
  private val repository = "scala3"
  private val odersky = "odersky"

  describe("The GitHubService") {
    describe("with paginated blocking behavior") {
      describe("when asked for repositories") {
        it("of an existing organization should return all of them") {
          Async.blocking:
            either:
              val repos = gitHubService.repositoriesOf(organization)
              repos.size should be > defaultNumberOfResultsPerPage
              repos.foreach(_.organization shouldBe organization)
              repos.count(_.name == repository) shouldBe 1
            .isRight shouldBe true
        }

        it("of a non-existing organization should fail") {
          Async.blocking:
            either:
              gitHubService.repositoriesOf(nonExistingOrganization)
            .isLeft shouldBe true
        }
      }

      describe("when asked for contributors of an existing repository") {
        it("should return all of them") {
          Async.blocking:
            either:
              val contributors = gitHubService.contributorsOf(organization, repository)
              contributors.size should be > defaultNumberOfResultsPerPage
              contributors.count(_.user == odersky) shouldBe 1
            .isRight shouldBe true
        }
      }
    }

    describe("with paginated incremental results") {
      describe("when asked for repositories") {
        it("of an existing organization should return all of them") {
          Async.blocking:
            val results = gitHubService.incrementalRepositoriesOf(organization).toSeq
            results.size should be > defaultNumberOfResultsPerPage
            results.foreach: r =>
              r.isRight shouldBe true
              r.toOption.get.organization shouldBe organization
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

    describe("with flowing results") {
      describe("when asked for repositories") {
        it("of an existing organization should return all of them") {
          var repos: Seq[Repository] = Seq.empty
          Async.blocking:
            val reposFlow = gitHubService.flowingRepositoriesOf(organization)
            reposFlow.collect: r =>
              r.isSuccess shouldBe true
              repos = repos :+ r.get
          repos.size should be > defaultNumberOfResultsPerPage
          repos.foreach(_.organization shouldBe organization)
          repos.count(_.name == repository) shouldBe 1
        }

        it("of a non-existing organization should fail") {
          Async.blocking:
            val reposFlow = gitHubService.flowingRepositoriesOf(nonExistingOrganization)
            reposFlow.collect:
              _.isFailure shouldBe true
        }

        it("for showcasing / 1") {
          Async.blocking:
            val reposFlow = gitHubService.flowingRepositoriesOf(organization)
            log("Still not collecting...")
            sleep(1000)
            log("Starting collecting...")
            reposFlow.collect(log)
            log("Done!")
        }

        it("for showcasing / 2") {
          Async.blocking:
            val reposFlow = gitHubService.flowingRepositoriesOf(nonExistingOrganization)
            log("Still not collecting...")
            sleep(1000)
            log("Starting collecting...")
            reposFlow.collect(log)
            log("Done!")
        }
      }
    }

    describe("when asked for the last release of an existing repository") {
      it("should return it if it exists") {
        Async.blocking:
          either:
            gitHubService.lastReleaseOf(organization, repository)
          .isRight shouldBe true
      }

      it("should fail if it does not exist") {
        Async.blocking:
          either:
            gitHubService.lastReleaseOf(organization, "dotty-website")
          .isLeft shouldBe true
      }
    }
  }

  private def log(x: Any): Unit = println(s"[${System.currentTimeMillis()}] $x")
}
