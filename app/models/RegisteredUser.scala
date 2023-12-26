package models


case class RegisteredUser(id: Int, username: String, password: String) extends UserTrait(username, password)

