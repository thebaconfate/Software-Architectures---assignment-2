package controllers

import javax.inject.Inject
import play.api.mvc.Results._
import play.api.mvc._
import services.AuthService

import scala.concurrent.{ExecutionContext, Future}

/**
 * Cobbled this together from:
 * https://www.playframework.com/documentation/2.6.x/ScalaActionsComposition#Authentication
 * https://www.playframework.com/documentation/2.6.x/api/scala/index.html#play.api.mvc.Results@values
 * `Forbidden`, `Ok`, and others are a type of `Result`.
 *
 * took code from wpo
 */
class AuthenticatedUserAction @Inject() (parser: BodyParsers.Default, authService: AuthService)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) {

  private val logger = play.api.Logger(this.getClass)

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    logger.info("ENTERED AuthenticatedUserAction::invokeBlock ...")
    val maybeJwt = request.session.get("jwt")
    val loggedIn = maybeJwt match {
      case None =>
        logger.info("No JWT found in session.")
        false
      case Some(jwt) =>
        logger.info(s"Found JWT in session: $jwt")
        authService.validateToken(jwt)
    }
    if loggedIn then
      val res: Future[Result] = block(request)
      res
    else
      Future.successful(Redirect(routes.HomeController.index()))
  }
}
