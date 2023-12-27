package controllers
import models.User
import play.api.data.*
import play.api.data.Forms.*
import play.api.mvc.*
import services.AuthService
import javax.inject.{Inject}

class UserController @Inject()(val cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger(this.getClass)
  private val userForm: Form[User] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
    )(User.apply)(User.unapply))


  def loginView: Action[AnyContent] = Action {
    implicit request: MessagesRequest[AnyContent] =>
      Ok(views.html.login("login", userForm, loginFormSubmitUrl))
  }
  private val loginFormSubmitUrl = routes.UserController.processLogin
  def processLogin: Action[AnyContent] = Action {
    implicit request: MessagesRequest[AnyContent] =>
      val errorFunction = {
        (formWithErrors: Form[User]) =>
          BadRequest(views.html.login("login", formWithErrors, loginFormSubmitUrl))
      }
      val successFunction = {
        (user: User) =>
          try {
            //val token = authService.loginUser(user)
            Redirect(routes.HomeController.index()).withSession("jwt" ->"token")
          }
          catch {
            case _: Exception => BadRequest(views.html.login("login", userForm, loginFormSubmitUrl))
          }
      }
      val formValidationResult = userForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

 /* def registerView: Action[AnyContent] = Action {
    implicit request =>
      Ok(views.html.register("register text", userForm))
  }

  def processRegister: Action[AnyContent] = Action { implicit request =>
    userForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          BadRequest(views.html.register("register", formWithErrors))
        },
        user => {
          try {
            //authService.registerUser(user)
            Redirect(routes.UserController.loginView)
          } catch {
            case _: Exception => BadRequest(views.html.register("register", userForm))
          }
        }
      )
  }*/

}