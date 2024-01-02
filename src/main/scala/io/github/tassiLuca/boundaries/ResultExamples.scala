package io.github.tassiLuca.boundaries

import io.github.tassiLuca.boundaries.result.{Ok, Result, Error}

object ResultExamples extends App:

  type User = String
  type Address = String

  def getUser(id: String): Result[User] =
    Ok("Mario Rossi")

  def getAddress(user: User): Result[Address] =
    Error("The user doesn't exists")

  def together(): Result[String] =
    result:
      val user = getUser("101").?
      val address = getAddress(user).?
      user + address

  println(together())
