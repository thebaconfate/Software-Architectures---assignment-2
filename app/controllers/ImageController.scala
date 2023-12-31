package controllers

import models.*
import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.Files
import services.{AuthService, ImageService}
import play.api.libs.Files.TemporaryFile

import javax.inject.Inject
import play.api.mvc.*

import java.nio.file.Paths

class ImageController @Inject()(val cc: ControllerComponents,
                                messagesAction: MessagesActionBuilder,
                                imageService: ImageService,
                                authService: AuthService) extends AbstractController(cc) {

  val imageUploadForm = Form(
    mapping(
      "description" -> nonEmptyText(maxLength = 240),
    )(ImageDescription.apply)(ImageDescription.unapply)
  )

  def specificImageView(username: String, imageURL: String): Action[AnyContent] = Action {
    implicit request =>
      if (authService.isAuthenticated(request)) {
        val sharedImage = imageService.getImage(username, imageURL)
        sharedImage match
          case None => NotFound("Image not found")
          case Some(actualSharedImage) => Ok(views.html.image(actualSharedImage))
      } else {
        Unauthorized(authService.unAuthMsg)
      }
  }

  def shareImageView: Action[AnyContent] = messagesAction {
    implicit request =>
      if (authService.isAuthenticated(request)) {
        Ok(views.html.shareImage(imageUploadForm))
      } else {
        Unauthorized(authService.unAuthMsg)
      }
  }

  def uploadImage: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { request =>
    /* Source: https://www.playframework.com/documentation/3.0.x/ScalaFileUpload */
    request.body
      .file("picture")
      .map { picture =>
        // only get the last part of the filename
        // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
        val filename    = Paths.get(picture.filename).getFileName
        val fileSize    = picture.fileSize
        val contentType = picture.contentType

        picture.ref.copyTo(Paths.get(s"/public/picture/$filename"), replace = false)
        Ok("File uploaded")
      }
      .getOrElse {
        Redirect(routes.HomeController.index()).flashing("error" -> "Missing file")
      }
  }
/*  def likeImage(username: String, imageURL: String): Action[AnyContent] = Action {
    implicit request =>
      val sharedImage = imageService.getImage(username, imageURL)
      sharedImage match
        case None => NotFound("Image not found")
        case Some(actualSharedImage) =>
          val updatedImage = imageService.likeImage(actualSharedImage)
          Ok(views.html.image(updatedImage))
  }*/


}
