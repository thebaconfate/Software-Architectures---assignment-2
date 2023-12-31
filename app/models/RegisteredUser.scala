package models

case class RegisteredUser(val id: Int, override val username: String, override val password: String) extends UserTrait
