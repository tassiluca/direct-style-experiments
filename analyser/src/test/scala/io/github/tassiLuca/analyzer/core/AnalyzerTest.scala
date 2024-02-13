//package io.github.tassiLuca.analyzer.core
//
//import gears.async.TaskSchedule.RepeatUntilFailure
//import gears.async.{Async, Task, TaskSchedule}
//import gears.async.default.given
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers
//
//class AnalyzerTest extends AnyFlatSpec with Matchers {
//
//  private val analyzer = Analyzer
//
//  "Analyze a organization" should "return a channel with all the repository results" in {
//    Async.blocking:
//      var results = Set[RepositoryReport]()
//      val (expectedResults, resultChannel) = analyzer.analyze("unibo-spe")
//      Task {
//        for _ <- 0 until expectedResults do results = results + resultChannel.read().toOption.get.await
//      }.run
//      Thread.sleep(10_000)
//      println(expectedResults)
//      expectedResults shouldBe 8
//      results.size shouldBe 8
//      println(results)
//  }
//}
