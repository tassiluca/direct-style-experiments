val values = (10, 1) :: (5, 2) :: (4, 2) :: (5, 1) :: Nil
println:
  try values.map(div)
  //         ^^^ map is the regular List.map implementation!
  catch case _: DivisionByZero => "Division by zero"