package controllers
import models.User
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.mvc.*
import services.AuthService

import javax.inject.{Inject, Singleton}

@Singleton
class UserController @Inject()(val cc: ControllerComponents, messagesAction: MessagesActionBuilder, authService: AuthService) extends AbstractController(cc) {

  private val passCheck: Constraint[String] = Constraint("constraints.passwordcheck")({
    plainText =>
      val passRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+])(?=\\S+$).{8,}$".r
      val errors = plainText match {
        case passRegex() => Nil
        case _ => Seq(ValidationError("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number and one special character"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
  })

  private val usernameCheck: Constraint[String] = Constraint("constraints.usernamecheck")({
    plainText =>
      val usernameRegex = "^[a-zA-Z0-9]+$".r
      val errors = plainText match {
        case usernameRegex() => Nil
        case _ => Seq(ValidationError("Username must contain only letters and numbers"))
      }
      if (authService.userExists(plainText)) {
        errors :+ ValidationError("Username already exists")
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
  })
  private val userForm: Form[User] = Form(
    mapping(
      "username" -> nonEmptyText(maxLength = 12, minLength = 6).verifying(usernameCheck),
      "password" -> nonEmptyText.verifying(passCheck),
    )(User.apply)(User.unapply))

  private val passwordForm: Form[String] = Form(
    mapping(
      "password" -> nonEmptyText.verifying(passCheck),
    )(identity)(Some(_))
  )


  def loginView: Action[AnyContent] = messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      if authService.isAuthenticated(request) then
        Redirect(routes.HomeController.index()) else
        Ok(views.html.login("login", userForm, false))
  }

  def logout: Action[AnyContent] = messagesAction{
    implicit request: MessagesRequest[AnyContent] =>
      Redirect(routes.UserController.loginView).withSession(request.session - authService.jwtKey)
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
            case _: Exception =>
              val formWithErrors = userForm.withError(FormError("username", "Username already exists"))
              BadRequest(views.html.register("register", formWithErrors, false))
          }
        }
      )
  }
  
  def profileView: Action[AnyContent] = messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      val user = authService.getUsername(request)
      user match
        case Some(username) => Ok(views.html.profile("profile", username, passwordForm))
        case None => Redirect(routes.UserController.loginView)
  }

  def processPassReset: Action[AnyContent]= messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      passwordForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            BadRequest(views.html.profile("profile", authService.getUsername(request).get, formWithErrors))
          },
          password => {
            authService.changePassword(authService.getUsername(request).get, password)
            Redirect(routes.UserController.profileView)
          }
        )
  }

}