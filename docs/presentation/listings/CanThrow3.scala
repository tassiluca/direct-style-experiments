def div(n: Int, m: Int): Int = m match
  case 0 => throw DivisionByZero()
  //        ^^^^^^^^^^^^^^^^^^^^^^
  // The capability to throw exception DivisionByZero is missing.
  case _ => n / m