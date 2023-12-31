package models

case class User(override val username: String, override val password: String) extends UserTrait

object User {
  def unapply(u: User): Option[(String, String)] = Some(u.username, u.password)
}