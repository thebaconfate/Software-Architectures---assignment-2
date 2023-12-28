package models
import java.util.Date

case class SharedImage(imagePath: String,
                       owner: Int,
                       addedDate: Date,
                       likes: Seq[Int],
                       description: String,
                       comments: Seq[Comment])
