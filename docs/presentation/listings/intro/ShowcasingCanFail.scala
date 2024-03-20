type CanFail = Label[Left[String, Nothing]]

def getUser(id: UserId)(using CanFail): User =
  val user = userBy(id)
  if verifyUser(user) then user else fail("Incorrect user")
  // fail is a shorthand for `break(Left("Incorrect user"))`

def getPayment(user: User)(using CanFail): PaymentMethod =
  paymentMethodOf(user) match
    case Some(a) if verifyMethod(a) => a
    case Some(_) => fail("The payment method is not valid")
    case _ => fail("Missing payment method")

def paymentData(id: UserId) = either:
  val user = getUser(id)
  val address = getPayment(user)
  (user, address)
