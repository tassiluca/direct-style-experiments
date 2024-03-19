package io.github.tassiLuca.dse.examples.boundaries

import io.github.tassiLuca.dse.boundaries.{CanFail, either}
import io.github.tassiLuca.dse.boundaries.either.leave

import scala.util.boundary
import scala.util.boundary.{Label, break}

trait EitherExamples:

  type User
  type UserId
  type Address

  def userBy(id: UserId): User
  def verifyUser(id: User): Boolean
  def addressOf(user: User): Option[Address]
  def verifyAddress(address: Address): Boolean

  def getUser(id: UserId)(using CanFail): User =
    val user = userBy(id)
    if verifyUser(user) then user else leave("Incorrent user")

  def getAddress(user: User)(using CanFail): Address =
    addressOf(user) match
      case Some(a) if verifyAddress(a) => a
      case Some(_) => leave("Incorrect address")
      case _ => leave("Missing address")

  def getShippingData(id: UserId) =
    either:
      val user = getUser(id)
      val address = getAddress(user)
      (user, address)
