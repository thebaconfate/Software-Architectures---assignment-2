package controllers

import models.*
import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.Files
import services.{AuthService, ImageService}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json

import javax.inject.Inject
import play.api.mvc.*

import java.nio.file.Paths

class ImageController @Inject()(val cc: ControllerComponents,
                                messagesAction: MessagesActionBuilder,
                                imageService: ImageService,
                                authService: AuthService) extends AbstractController(cc) {

  private val uploadCommentForm: Form[UploadComment] = Form(
    mapping(
      "comment" -> nonEmptyText.verifying("Comment must be less than 100 characters", comment => comment.length <= 250)
    )(UploadComment.apply)(UploadComment.unapply)
  )

  def specificImageView(username: String, imageURL: String): Action[AnyContent] = messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      if (authService.isAuthenticated(request)) {
        val sharedImage = imageService.getImage(username, imageURL)
        sharedImage match
          case None => NotFound("Image not found")
          case Some(actualSharedImage) => Ok(views.html.image(actualSharedImage, uploadCommentForm))
      } else {
        Unauthorized(authService.unAuthMsg)
      }
  }

  def shareImageView: Action[AnyContent] = messagesAction {
    implicit request =>
      if (authService.isAuthenticated(request)) {
        Ok(views.html.shareImage())
      } else {
        Unauthorized(authService.unAuthMsg)
      }
  }

  def uploadImage: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) {
    request =>
      /* Source: https://www.playframework.com/documentation/3.0.x/ScalaFileUpload */
      request.body
        .file("picture")
        .map { picture =>
          // only get the last part of the filename
          // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
          val filename    = Paths.get(picture.filename).getFileName
          val fileSize    = picture.fileSize
          val contentType = picture.contentType
          val userName = authService.getUsername(request)
          userName match
            case None =>
              Unauthorized(authService.unAuthMsg)
            case Some(actualUserName) =>
              val description = request.body.dataParts.get("description").flatMap(_.headOption).getOrElse("")
              val imagePath = s"$actualUserName/$filename"
              val dateAdded = java.util.Date()
              val likes = List[Int]()
              val comments = List[Comment]()
              val image = SharedImage(s"$filename", actualUserName, dateAdded, likes, description, comments)
              imageService.addImage(image)
              picture.ref.copyTo(Paths.get(s"./public/images/$imagePath"), replace = true)
              Redirect(routes.HomeController.index()).flashing("success" -> "Image uploaded successfully")
        }
        .getOrElse {
          Redirect(routes.HomeController.index()).flashing("error" -> "Missing file")
        }
  }
  def likeImage(owner: String, imageURL: String): Action[AnyContent] = Action {
    implicit request =>
      val username = authService.getUserID(request)
      username match
        case None =>
          Unauthorized(authService.unAuthMsg)
        case Some(actualUsername) =>
          try {
            val liked = imageService.likeImage(owner, imageURL, actualUsername)
            Ok(Json.obj("likesCount" -> liked))
          } catch {
            case e: Exception =>
              BadRequest(e.getMessage)
          }
  }

  def addComment(owner: String, imageURL: String): Action[AnyContent] = messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      val sharedImage = imageService.getImage(owner, imageURL).get
      val username = authService.getUsername(request)
      username match
        case None =>
          Unauthorized(authService.unAuthMsg)
        case Some(actualUsername) =>
          uploadCommentForm
            .bindFromRequest()
            .fold(
              formWithErrors => {
                BadRequest(views.html.image(sharedImage, formWithErrors))
              },
              comment => {
                try {
                  val formattedComment = Comment(username.get, comment.content)
                  imageService.addComment(owner, imageURL, formattedComment)
                  Redirect(routes.ImageController.specificImageView(owner, imageURL))
                } catch {
                  case _: Exception =>
                    val formWithErrors = uploadCommentForm.withError(FormError("comment", "Invalid comment"))
                    BadRequest(views.html.image(sharedImage, formWithErrors))
                }
              }
            )

  }


}
