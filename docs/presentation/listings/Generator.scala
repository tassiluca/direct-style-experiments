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