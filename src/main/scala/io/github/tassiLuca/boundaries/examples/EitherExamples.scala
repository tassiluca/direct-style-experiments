package io.github.tassiLuca.boundaries.examples

import scala.util.boundary
import scala.util.boundary.{Label, break}

trait EitherExamples:

  type User
  type UserId
  type Address

  def findUser(id: UserId): User
  def verifyUser(id: User): Boolean
  def findAddresses(user: User): Option[Address]
  def verifyAddress(address: Address): Boolean

  def getUser(id: UserId)(using Label[Left[String, Nothing]]): User =
    val user = findUser(id)
    if verifyUser(user) then user else break(Left("Incorrent user"))

  def getAddress(user: User)(using Label[Left[String, Nothing]]): Address =
    findAddresses(user) match
      case Some(a) if verifyAddress(a) => a
      case Some(_) => break(Left("Incorrect address"))
      case _ => break(Left("Missing address"))

  def getShippingData(id: UserId): Either[String, (User, Address)] =
    boundary:
      val user = getUser(id)
      val address = getAddress(user)
      Right((user, address))
