package models

case class User(username: String, password: String) extends UserTrait(username, password)
object User{
  def unapply(u: User): Option[(String, String)] = Some(u.username, u.password)
}