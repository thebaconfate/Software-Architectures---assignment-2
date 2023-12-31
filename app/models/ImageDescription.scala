package models

case class ImageDescription(description: String)

object ImageDescription {
  def unapply(i: ImageDescription): Option[String] = Some(i.description)
}