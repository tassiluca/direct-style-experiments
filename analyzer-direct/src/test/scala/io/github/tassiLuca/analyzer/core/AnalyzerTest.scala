package io.github.tassiLuca.analyzer.core

import gears.async.Async
import io.github.tassiLuca.analyzer.lib.Analyzer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AnalyzerTest extends AnyFlatSpec with Matchers {

  private val analyzer = Analyzer.ofGitHub
}
