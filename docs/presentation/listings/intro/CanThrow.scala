import language.experimental.saferExceptions // important!
class DivisionByZero extends Exception

def div(n: Int, m: Int)(using CanThrow[DivisionByZero]): Int = 
  m match
    case 0 => throw DivisionByZero()
    case _ => n / m

println:
  try
    // the compiler generates an accumulated capability as follows:
    // erased given CanThrow[DivisionByZero] = compiletime.erasedValue
    div(10, 0)
  catch case _: DivisionByZero => "Division by zero"

val values = (10, 1) :: (5, 2) :: (4, 2) :: (5, 1) :: Nil
println:
  try values.map(div) // map is the regular List.map implementation!
  catch case _: DivisionByZero => "Division by zero"