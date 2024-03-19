class DivisionByZero extends Exception

// or equivalently, `CanThrow[DivisionByZero] ?=> Int`  
def div(n: Int, m: Int)(using CanThrow[DivisionByZero]): Int = 
  m match
    case 0 => throw DivisionByZero()
    case _ => n / m

println: // "Division by zero"
  try div(10, 0)
  catch case _: DivisionByZero => "Division by zero"
