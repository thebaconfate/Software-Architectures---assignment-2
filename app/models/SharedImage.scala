package models
import java.util.Date

case class SharedImage(imagePath: String,
                       owner: String,
                       addedDate: Date,
                       likes: Seq[Int],
                       description: String,
                       comments: Seq[Comment]) {

  def getTitle: String = {
    imagePath.split("/").last.split("\\.").head
  }
}
