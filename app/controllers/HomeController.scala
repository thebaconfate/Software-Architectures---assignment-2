package controllers

import models.SharedImage

import javax.inject.*
import play.api.*
import play.api.mvc.*
import services.*
import models.*

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               imageService: ImageService,
                               authService: AuthService) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(sortBy: String, sorting: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      val authenticated = authService.isAuthenticated(request)

      val sharedImageList: List[SharedImage] = if authenticated then
        imageService.getImages
      else
        List()
      val sortedImageList = sortBy match
        case "date" => sharedImageList.sortBy(_.addedDate)
        case "likes" => sharedImageList.sortBy(_.likes.length)
        case _ => sharedImageList
      val resultImages = sorting match
        case "desc" => sortedImageList.reverse
        case _ => sortedImageList
      println(s"sortBy: $sortBy, sorting: $sorting")
      for (image <- resultImages) {
        println(s"image: $image")
      }
      Ok(views.html.index("Some text", resultImages, sortBy, sorting))

  }
}
