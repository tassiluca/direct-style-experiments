package io.github.tassiLuca.boundaries

import io.github.tassiLuca.boundaries.BoundaryExamples.firstIndex
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.shouldBe

class BoundaryExamplesTest extends AnyFlatSpec:
  "BoundaryExamples.firstIndex" should "return the first index of an item in the list or -1 if not present" in {
    firstIndex(List(1, 2, 3, 4), 4) shouldBe 3
    firstIndex(List(1, 2, 3, 4), 0) shouldBe -1
  }
