package controllers

import models.SharedImage

import javax.inject.*
import play.api.*
import play.api.mvc.*
import services._

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
  def index(): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      val authenticated = authService.isAuthenticated(request)
      val sharedImageList: List[SharedImage] = if authenticated then
        imageService.getImages
      else
        List()
      for(i <- sharedImageList) println(i.imagePath)
      Ok(views.html.index("Some text", sharedImageList))

  }
}
