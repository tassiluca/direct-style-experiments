package io.github.tassiLuca.dse.examples.boundaries

import io.github.tassiLuca.dse.boundaries.{CanFail, either}
import io.github.tassiLuca.dse.boundaries.either.fail

import scala.util.boundary
import scala.util.boundary.{Label, break}

trait ShowcasingCanFail:

  type User
  type UserId
  type PaymentMethod

  def userBy(id: UserId): User
  def verifyUser(id: User): Boolean
  def paymentMethodOf(user: User): Option[PaymentMethod]
  def verifyMethod(address: PaymentMethod): Boolean

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
