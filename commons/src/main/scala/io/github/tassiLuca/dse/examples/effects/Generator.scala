package io.github.tassiLuca.dse.examples.effects

import gears.async.VThreadSupport.{boundary, suspend}
import Tree.{Inner, Leaf}

enum Tree[T]:
  case Leaf(x: T)
  case Inner(xs: List[Tree[T]])

trait Generator[T]:
  def nextOption: Option[T]

// What in Koka we would call effect produce<t>
trait Produce[-T]:
  def produce(x: T): Unit

def generate[T](body: (it: Produce[T]) ?=> Unit) = new Generator[T]:
  override def nextOption: Option[T] = step()

  var step: () => Option[T] = () =>
    boundary:
      // what in Koka we would call handler
      given Produce[T] with
        override def produce(x: T): Unit =
          suspend[Unit, Option[T]]: k =>
            step = () => k.resume(())
            Some(x)
      body
      None

object Tree:
  extension [T](t: Tree[T])
    def leafs: Generator[T] = generate:
      def recur(t: Tree[T]): Unit = t match
        case Leaf(x) => it.produce(x)
        case Inner(xs) => xs.foreach(recur)
      recur(t)

@main def useTree(): Unit =
  /*
            *
          / | \
         1  *  5
           / \
          2   3
   */
  val tree = Tree.Inner(
    Leaf(1) :: Inner(
      Leaf(2) :: Leaf(3) :: Nil
    ) :: Leaf(5) :: Nil
  )
  val leafs = tree.leafs
  for _ <- 0 to 4 do println(leafs.nextOption)
