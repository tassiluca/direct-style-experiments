package io.github.tassiLuca.core

import io.github.tassiLuca.utils.*

/** The N-d space on which the game world is absorbed. */
trait Space:

  type Position
  type Speed
  type Shape

  def world: World
  def world_=(world: World): Unit

  sealed trait Entity:
    val position: Position
    val speed: Speed
    val shape: Shape

  class Player(override val position: Position, override val speed: Speed, override val shape: Shape) extends Entity
  class Obstacle(override val position: Position, override val speed: Speed, override val shape: Shape) extends Entity

  case class World(player: Player, obstacles: Set[Obstacle], shape: Shape)

  extension (e: Entity) def isCollidingWith(e2: Entity): Boolean

/** A 2 dimensional space. */
trait Space2D extends Space:
  override type Speed = Vector2D
  override type Position = Point2D

/** A mixin specifying rectangular shapes along with the definition of collision among them. */
trait RectangularEntities:
  self: Space2D =>

  case class RectangularShape(width: Int, height: Int)

  override type Shape = RectangularShape

  extension (e: Entity)
    override def isCollidingWith(e2: Entity): Boolean =
      (e.position._1 <= e2.position._1 + e2.shape.width) && (e.position._1 + e.shape.width >= e2.position._1) &&
        (e.position._2 <= e2.position._2 + e2.shape.height) && (e.position._2 + e.shape.height >= e2.position._2)

object Space2D:
  def apply(size: Point2D): Space2D with RectangularEntities = new Space2D with RectangularEntities:
    var _world: World = World(
      player = Player(size / 2, (0, 1.0), RectangularShape(size._1 / 10, size._2 / 10)),
      obstacles = Set.empty,
      shape = RectangularShape(size._1, size._2),
    )
    override def world: World = _world
    override def world_=(world: World): Unit = _world = world
