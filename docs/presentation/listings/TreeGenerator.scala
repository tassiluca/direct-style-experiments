enum Tree[T]:
  case Leaf(x: T)
  case Inner(xs: List[Tree[T]])

object Tree:
  extension[T](t: Tree[T])
    def leafs: Generator[T] = generate:
      def recur(t: Tree[T]): Unit = t match
        case Leaf(x) => it.produce(x)
        case Inner(xs) => xs.foreach(recur)
      recur(t)
