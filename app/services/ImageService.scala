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
  private val imagesDBFile = File(imagesJsonFile)

  def getImages: List[SharedImage] = {

    val inputStream = FileInputStream(imagesDBFile)
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

  def addImage(newImage: SharedImage): Unit = {
    val exists = getImage(newImage.owner, newImage.imagePath)
    exists match {
      case Some(image) => throw new Exception("image already exists")
      case None => {
        val images = getImages.::(newImage)
        saveImages(images)
      }
    }
  }

  private def saveImages(images: List[SharedImage]): Unit = {
    val json = Json.toJson(images)
    val outputStream = java.io.FileOutputStream(imagesDBFile)
    try {
      outputStream.write(Json.prettyPrint(json).getBytes)
      outputStream.flush()
      outputStream.close()
    } finally {
      outputStream.close()
    }
  }

  def likeImage(imageOwner: String, imagePath: String, liker: Int): Int = {
    def addImageToJson(images : List[SharedImage], newImage: SharedImage) = {
      images.filter(image => !(image.owner == imageOwner && image.imagePath == imagePath)) :+ newImage
    }
    val images = getImages
    val image = images.find(image => image.owner == imageOwner && image.imagePath == imagePath)
    image match {
      case Some(image) =>
        if (!image.likes.contains(liker)) {
          val newImage = image.copy(likes = image.likes :+ liker)
          val newImages = addImageToJson(images, newImage)
          saveImages(newImages)
          newImage.likes.length
        } else {
          val newImage = image.copy(likes = image.likes.filter(like => like != liker))
          val newImages = addImageToJson(images, newImage)
          saveImages(newImages)
          newImage.likes.length
        }
      case None => throw new Exception("image not found")
    }
  }

  def addComment(imageOwner: String, imagePath: String, comment: Comment): Unit = {
    val images = getImages
    val image = images.find(image => image.owner == imageOwner && image.imagePath == imagePath)
    image match {
      case Some(image) => {
        val newImage = image.copy(comments = image.comments :+ comment)
        val newImages = images.filter(image => !(image.owner == imageOwner && image.imagePath == imagePath)) :+ newImage
        saveImages(newImages)
      }
      case None => throw new Exception("image not found")
    }
  }
}
