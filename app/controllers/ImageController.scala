package controllers

import models.SharedImage
import play.api.mvc.{AbstractController, Action, AnyContent, BaseController, ControllerComponents}
import services.{AuthService, ImageService}

import javax.inject.Inject

class ImageController @Inject()(val cc: ControllerComponents,
                                imageService: ImageService,
                                authService: AuthService) extends AbstractController(cc) {

  def specificImageView(username: String, imageURL: String): Action[AnyContent] = Action {
    implicit request =>
      val sharedImage = imageService.getImage(username, imageURL)
      sharedImage match
        case None => NotFound("Image not found")
        case Some(actualSharedImage) => Ok(views.html.image(actualSharedImage))
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
