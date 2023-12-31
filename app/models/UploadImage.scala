package models

import play.api.libs.Files.TemporaryFile

case class UploadImage(image: TemporaryFile, description : String) {
  def unapply(i: UploadImage): Option[(TemporaryFile, String)] = Some(i.image, i.description)
}