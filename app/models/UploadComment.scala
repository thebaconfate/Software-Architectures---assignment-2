package models

case class UploadComment(content: String)

object UploadComment {
  def unapply(comment: UploadComment): Option[String] =
    Some(comment.content)
}