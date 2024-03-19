/** Represents a computation that will hopefully return a [[Right]] value, but might fail with a [[Left]] one. */
object either:

  inline def apply[L, R](inline body: Label[Left[L, Nothing]] ?=> R): Either[L, R] =
    boundary(Right(body))

  inline def left[L, R](l: L)(using Label[Left[L, R]]): R = break(Left(l))

  extension [L, R](e: Either[L, R])
    inline def ?(using Label[Left[L, Nothing]]): R = e match
      case Right(value) => value
      case Left(value) => break(Left(value))
