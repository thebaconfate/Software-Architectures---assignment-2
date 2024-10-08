package controllers
import models.User
import play.api.data.*
import play.api.data.Forms.*
import play.api.mvc.*
import services.AuthService
import javax.inject.{Inject, Singleton}

@Singleton
class UserController @Inject()(val cc: ControllerComponents, messagesAction: MessagesActionBuilder, authService: AuthService) extends AbstractController(cc) {

  private val userForm: Form[User] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
    )(User.apply)(User.unapply))


  def loginView: Action[AnyContent] = messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      if authService.isAuthenticated(request) then
        Redirect(routes.HomeController.index()) else
        Ok(views.html.login("login", userForm, false))
  }

  def logout: Action[AnyContent] = messagesAction{
    implicit request: MessagesRequest[AnyContent] =>
      Redirect(routes.UserController.loginView).withNewSession
  }
  def processLogin: Action[AnyContent] = messagesAction { implicit request =>
    userForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          BadRequest(views.html.login("login", formWithErrors, false))
        },
        user => {
          try {
            val token = authService.loginUser(user)
            Redirect(routes.HomeController.index()).withSession(authService.jwtKey -> token)
          } catch {
            case _: Exception => BadRequest(views.html.login("login", userForm, false))
          }
        }
      )
  }

  def registerView: Action[AnyContent] = messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      Ok(views.html.register("register text", userForm, false))
  }

  def processRegister: Action[AnyContent] = messagesAction { implicit request =>
    userForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          BadRequest(views.html.register("register", formWithErrors, false))
        },
        user => {
          try {
            val formattedUser = user.copy(username = user.username.toLowerCase())
            authService.registerUser(formattedUser)
            Redirect(routes.UserController.loginView)
          } catch {
            case _: Exception => BadRequest(views.html.register("register", userForm, false))
          }
        }
      )
  }

}