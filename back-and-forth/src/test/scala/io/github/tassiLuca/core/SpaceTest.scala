package io.github.tassiLuca.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SpaceTest extends AnyFlatSpec with Matchers:

  "Creating a 2D space" should "initialize a world of the given size with just a player and no obstacles" in {
    val size = (100, 150)
    val space = Space2D(size)
    space.world.player.position should be < size
    space.world.obstacles.size shouldBe 0
    space.world.shape.width shouldBe size._1
    space.world.shape.height shouldBe size._2
  }

  "It" should "be possible to update the world" in {}

  "Checking collision between two 2D rectangular shapes" should "work" in {
    val space = Space2D((10, 10))
    space.world.player.position shouldBe (5, 5)
    space.world.player.shape shouldBe space.RectangularShape(1, 1)
    val collidingObstacles = Set(
      space.Obstacle((4, 3), (0, 0), space.RectangularShape(1, 2)),
      space.Obstacle((5, 6), (0, 0), space.RectangularShape(1, 2)),
      space.Obstacle((6, 5), (0, 0), space.RectangularShape(2, 1)),
    )
    val nonCollidingObstacles = Set(
      space.Obstacle((0, 0), (0, 0), space.RectangularShape(1, 2)),
      space.Obstacle((7, 6), (0, 0), space.RectangularShape(3, 3)),
    )
    space.world = space.World(space.world.player, collidingObstacles ++ nonCollidingObstacles, space.world.shape)
    collidingObstacles.foreach(o => space.isCollidingWith(space.world.player)(o) shouldBe true)
    nonCollidingObstacles.foreach(o => space.isCollidingWith(space.world.player)(o) shouldBe false)
  }
