// Non local returns are no longer supported!
def deprecatedFirstIndex[T](xs: List[T], elem: T): Int =
  for (x, i) <- xs.zipWithIndex do if x == elem then return i
  -1

def firstIndex[T](xs: List[T], elem: T): Int =
  boundary:
    for (x, i) <- xs.zipWithIndex do if x == elem then break(i)
    -1