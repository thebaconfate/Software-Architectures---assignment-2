package services

import javax.inject.Inject
import models.*
import play.api.libs.json.*
import play.api.libs.functional.syntax.*
import java.util.Date
import java.io.{File, FileInputStream}
class ImageService @Inject()(){
  private val imagesJsonFile = "./resources/images.json"
  private val imagesFilesFolder = "./public/images/"
  implicit val commentFormat: Format[Comment] = (
      (JsPath \ "comment_owner").format[String] and
      (JsPath \ "comment"  ).format[String]
    )(Comment.apply, c => (c.owner, c.content))
  implicit val sharedImageFormat: Format[SharedImage] = (
    (JsPath \ "image_path").format[String] and
      (JsPath \ "image_owner").format[String] and
      (JsPath \ "added_date").format[Date] and
      (JsPath \ "likes").format[Seq[Int]] and
      (JsPath \ "description").format[String] and
      (JsPath \ "comments").format[Seq[Comment]]
    )(SharedImage.apply, si => (si.imagePath, si.owner, si.addedDate, si.likes, si.description, si.comments))

  def getImages: List[SharedImage] = {
    println("reading images from json file")
    val file = File(imagesJsonFile)
    val inputStream = FileInputStream(file)
    println("file found")
    try {
      Json.parse(inputStream).as[List[SharedImage]]
    } finally {
      inputStream.close()
    }
  }
}
