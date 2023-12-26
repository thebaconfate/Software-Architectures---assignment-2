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
class UserController @Inject()(val cc: ControllerComponents, messagesAction: MessagesActionBuilder) extends AbstractController(cc) {

  val loginForm: Form[User] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
    )(User.apply)(User.unapply))

  def loginView: Action[AnyContent] = messagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      Ok(views.html.login("login", loginForm))
  }

  def processLogin: Action[AnyContent] = messagesAction { implicit request =>
    loginForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          BadRequest(views.html.login("login", formWithErrors))
        },
        user => {
          try {
            val token = AuthService.loginUser(user)
            Redirect(routes.HomeController.index()).withSession("jwt" -> token)
          } catch {
            case _: Exception => BadRequest(views.html.login("login", loginForm))
          }
        }
      )
  }
  

  def registerView: Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      Ok(views.html.register("register text"))
  }
  
  /*def processReview: Action[AnyContent] = MessagesAction {
    implicit request: MessagesRequest[AnyContent] =>
      registerForm 
  }*/

}