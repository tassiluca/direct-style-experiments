try 
  // `erased`: definition that is erased before code generation
  erased given ctl: CanThrow[DivisionByZero] = compiletime.erasedValue
  div(10, 0)(using ctl)
catch case _: DivisionByZero => "Division by zero"
