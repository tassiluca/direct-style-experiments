trait Suspension[-T, +R]:
  def resume(arg: T): R

trait SuspendSupport:
  def suspend[T, R](body: Suspension[T, R] => R)(using Label[R]): T