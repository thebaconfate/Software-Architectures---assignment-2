package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}

@Singleton
class UserController @Inject()(val cc: ControllerComponents) extends AbstractController(cc) {

  def loginView: Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
    Ok(views.html.login("login text"))
  }

  def registerView: Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
    Ok(views.html.register("register text"))
  }


}