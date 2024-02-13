/*package io.github.tassiLuca.analyzer.core

import gears.async.Async
import gears.async.default.given
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GitHubServiceTest extends AnyFlatSpec with Matchers {

  val gitHubService: GitHubService = GitHubService()

  "organizationsRepositories" should "return all the repositories of a given organization" in {
    Async.blocking:
      val result = gitHubService.organizationsRepositories("unibo-disi-cesena")
      println(result)
  }

  "contributorsOf" should "return all the contributors of a given repository" in {
    Async.blocking:
      val result = gitHubService.contributorsOf("unibo-disi-cesena", "thesis-template")
      println(result)
  }

  "languagesOf" should "return all languages used in a given repository" in {
    Async.blocking:
      val result = gitHubService.languagesOf("unibo-disi-cesena", "thesis-template")
      println(result)
  }
}
 */
