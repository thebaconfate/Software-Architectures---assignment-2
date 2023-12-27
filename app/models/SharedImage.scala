package models
import java.util.Date

case class SharedImage(imagePath: String,
                       owner: Int,
                       addedDate: Date,
                       likes: List[Int],
                       description: String,
                       comments: List[Comment])
