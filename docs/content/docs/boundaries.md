# `boundary / break`

[Source code](https://github.com/lampepfl/dotty/blob/3.3.0-RC4/library/src/scala/util/boundary.scala)

- Provides a cleaner alternative to non-local returns;
  - `boundary:` is short for `boundary.apply:` with the indented code below it passed as the body
    - `block` is a context function that is called within boundary.apply to return the block of code shown in the example
    - Users don’t define `Label` instances themselves. Instead, this is done inside the implementation of `boundary.apply` to provide the capability of doing a non-local return.

      ```scala
      /** Run `body` with freshly generated label as implicit argument. Catch any
       *  breaks associated with that label and return their results instead of
       *  `body`'s result.
       */
      inline def apply[T](inline body: Label[T] ?=> T): T =
        val local = Label[T]()
        try body(using local)
        catch case ex: Break[T] @unchecked =>
          if ex.label eq local then ex.value
          else throw ex
      ```
      
      - we don’t want users to call break without an enclosing boundary. That’s why break requires an in-scope given instance of Label, which the implementation of boundary.apply creates before it calls the code block you provide. If your code block calls break, a given Label will be in-scope.
  - non-localbreaks are logically implemented as non-fatal exceptions and the implementation is optimized to suppress unnecessary stack trace generation. Stack traces are unnecessary because we are handling these exceptions, not barfing them on the user!
  - optimizations: Better performance is provided when a break occurs to the enclosing scope inside the same method (i.e., the same stack frame), where it can be rewritten to a jump call.

## What can we do with boundary and break mechanism?

### `Optional`

```scala
object optional:

  inline def apply[T](inline body: Label[None.type] ?=> T): Option[T] =
    boundary(Some(body))

  extension [T](o: Option[T])
    inline def ?(using label: Label[None.type]): T =
      o.getOrElse(break(None))
```

### Rust-like `Result` + `?`

```scala
object result:

  sealed trait Result[+T]
  case class Ok[+T](t: T) extends Result[T]
  case class Error(e: String) extends Result[Nothing]

  inline def apply[T](inline body: Label[Error] ?=> T): Result[T] =
    boundary(Ok(body))

  extension [T](r: Result[T])
    inline def ?(using Label[Error]): T = r match
      case Ok(t) => t
      case e @ Error(_) => break(e)
```

### `Either` + `?`

```scala
object either:

  inline def apply[L, R](inline body: Label[Left[L, Nothing]] ?=> R): Either[L, R] =
    boundary(Right(body))

  extension [L, R](e: Either[L, R])
    inline def ?(using Label[Left[L, Nothing]]): R = e match
      case Right(value) => value
      case Left(value) => break(Left(value))
```