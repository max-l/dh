package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import com.strong_links.crypto.ToughCookieBakery
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import com.strong_links.crypto.ToughCookieStatus

object Secured {
  
  val authenticator = 
    Play.current.configuration.getString("application.secret").
      map(new ToughCookieBakery(_)).getOrElse(sys.error("missing config 'application.secret'"))

}

trait Secured {

  import Secured._
  
  private val authenticatonTokenName = "authenticatonToken"


  private def validateToken(request: RequestHeader) = 
    for(authenticatonToken <-request.session.get(authenticatonTokenName); 
        userId <- authenticator.validate(authenticatonToken,sslSessionId((request))) match {
          case (ToughCookieStatus.Valid, Some((_, _, userIdInCookie))) => Some(userIdInCookie)
          case _ => None
        }
    )
    yield userId

  protected def sslSessionId(request: RequestHeader) = "fwe12342fewf343434"
  
  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)
  
  // --
  
  val maxIdleTimeInSeconds = 60 * 7
  
  /** 
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(validateToken, onUnauthorized) { userId =>
      Action { request => 
        val t = authenticator.bake(userId, maxIdleTimeInSeconds, sslSessionId(request), "")
        f(userId)(request).asInstanceOf[PlainResult].withSession(authenticatonTokenName -> t)
      }
    }
  }

  def AuthenticationSuccess(r: Result, username : String)(implicit req: Request[AnyContent]) = {

    val t = authenticator.bake(username, maxIdleTimeInSeconds, sslSessionId(req), "")
    r.asInstanceOf[PlainResult].withSession(req.session + (authenticatonTokenName -> t))
  }
/*
  def IsMemberOf(project: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
    if(Project.isMember(project, user)) {
      f(user)(request)
    } else {
      Results.Forbidden
    }
  }

  /**
   * Check if the connected user is a owner of this task.
   */
  def IsOwnerOf(task: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
    if(Task.isOwner(task, user)) {
      f(user)(request)
    } else {
      Results.Forbidden
    }
  }
*/
}
