package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import services.AuthService

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
/*    val token = request.session.get("jwt")
    print(token)
    val loggedIn = token match {
      case Some(t) => authService.loggedIn(t)
      case None => false
    }
    println(s"Logged in: $loggedIn")*/
    Ok(views.html.index("Home"))
  }
}
