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
  
  def getImage(username: String, imageURL: String): Option[SharedImage] = {
    val images = getImages
    images.find(image => image.owner == username && image.imagePath == imageURL)
  }

  def saveImages(value: List[SharedImage]): Unit = {
    val file = File(imagesJsonFile)
    val outputStream = java.io.FileOutputStream(file)
    try {
      outputStream.write(Json.toJson(value).toString().getBytes)
    } finally {
      outputStream.close()
    }
  }
  def addComment(imageOwner: String, imagePath: String, comment: String): Unit = {
    val images = getImages
    val image = images.find(image => image.owner == imageOwner && image.imagePath == imagePath)
    image match {
      case Some(image) => {
        val newComment = Comment(imageOwner, comment)
        val newImage = image.copy(comments = image.comments :+ newComment)
        val newImages = images.filter(image => image.owner != imageOwner || image.imagePath != imagePath) :+ newImage
        saveImages(newImages)
      }
      case None => println("image not found")
    }
  }
}
