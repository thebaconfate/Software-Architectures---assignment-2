package models


case class RegisteredUser(username: String, password: String, id: Int) extends UserTrait(username, password)

