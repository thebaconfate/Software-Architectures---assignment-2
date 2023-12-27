package controllers

import models.User

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, MessagesAbstractController, MessagesActionBuilder, MessagesControllerComponents, MessagesRequest, Request}
import play.api.Configuration
import play.api.data.*
import play.api.data.Forms.*
import pdi.jwt.*
import play.api.data.Forms.nonEmptyText
import play.api.libs.json.{JsObject, Json}
import services.AuthService
import views.html.helper.form

import java.time.Clock

@Singleton
class UserController @Inject()(val cc: ControllerComponents, messagesAction: MessagesActionBuilder, authService: AuthService) extends AbstractController(cc) {

  private val userForm: Form[User] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
    )(User.apply)(User.unapply))
  

  def loginView: Action[AnyContent] = messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      Ok(views.html.login("login", userForm))
  }

  def processLogin: Action[AnyContent] = messagesAction { implicit request =>
    userForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          BadRequest(views.html.login("login", formWithErrors))
        },
        user => {
          try {
            val token = authService.loginUser(user)
            Redirect(routes.HomeController.index()).withSession("jwt" -> token)
          } catch {
            case _: Exception => BadRequest(views.html.login("login", userForm))
          }
        }
      )
  }
  
  def registerView: Action[AnyContent] = messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      Ok(views.html.register("register text", userForm))
  }
  
  def processRegister: Action[AnyContent] = messagesAction { implicit request =>
    userForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          BadRequest(views.html.register("register", formWithErrors))
        },
        user => {
          try {
            authService.registerUser(user)
            Redirect(routes.UserController.loginView)
          } catch {
            case _: Exception => BadRequest(views.html.register("register", userForm))
          }
        }
      )
  }

}