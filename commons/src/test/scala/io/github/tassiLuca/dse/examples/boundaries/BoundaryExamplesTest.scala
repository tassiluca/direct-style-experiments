package io.github.tassiLuca.dse.examples.boundaries

import io.github.tassiLuca.dse.examples.boundaries.BoundaryExamples.{firstColumn, firstColumn2, firstIndex}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.shouldBe

class BoundaryExamplesTest extends AnyFlatSpec:
  "BoundaryExamples.firstIndex" should "return the first index of an item in the list or -1 if not present" in {
    firstIndex(List(1, 2, 3, 4), 4) shouldBe 3
    firstIndex(List(1, 2, 3, 4), 0) shouldBe -1
  }

  "BoundaryExamples.firstColumn" should "return a List with the first element of each List given in input" in {
    firstColumn(List(List(1, 2, 3), List(3))) shouldBe Some(List(1, 3))
    firstColumn(List(List(), List(1))) shouldBe None
  }

  "BoundaryExamples.firstColumn2" should "return a List with the first element of each List given in input" in {
    firstColumn2(List(List(1, 2, 3), List(3))) shouldBe Some(List(1, 3))
    firstColumn2(List(List(), List(1))) shouldBe None
  }
