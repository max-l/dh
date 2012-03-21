package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import com.strong_links.crypto.ToughCookieBakery
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import com.strong_links.crypto.ToughCookieStatus
import models.User


object Secured {
  
  val authenticator = 
    Play.current.configuration.getString("application.secret").
      map(new ToughCookieBakery(_)).getOrElse(sys.error("missing config 'application.secret'"))

}

case class DecisionHubSession(userId: Long, dataInCookie: String, requestHeaders: RequestHeader) {
  def this(u: User, r: Request[_]) = this(u.id, u.displayableName.get, r)
  
  def displayName = dataInCookie
}

trait ConcreteSecured extends Secured[DecisionHubSession] {

  private def parseUserId(s: String) = 
    try {
      java.lang.Long.parseLong(s)
    }
    catch {
      case _ => sys.error("UserId is not a long '" + s + "'.")
    }
  
  def loadSession(userId: String, dataInCookie: String, h: RequestHeader) = DecisionHubSession(parseUserId(userId), dataInCookie, h)
  def userIdFromSession(s: DecisionHubSession) = s.userId.toString
  def dataFromSession(s: DecisionHubSession) = s.dataInCookie
}


trait Secured[S] {

  import Secured._
  
  
  def loadSession(userId: String, dataInCookie: String, h: RequestHeader): S
  def userIdFromSession(s: S): String
  def dataFromSession(s: S): String
  
  private val authenticatonTokenName = "authenticatonToken"


  private def validateToken(request: RequestHeader) = 
    for(authenticatonToken <-request.session.get(authenticatonTokenName); 
        userId <- authenticator.validate(authenticatonToken,sslSessionId((request))) match {
          case (ToughCookieStatus.Valid, Some((_, dataInCookie, userIdInCookie))) => Some(loadSession(userIdInCookie,dataInCookie, request))
          case _ => None
        }
    )
    yield userId

  protected def sslSessionId(request: RequestHeader) = "fwe12342fewf343434"
  

  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)
    
  val maxIdleTimeInSeconds = 60 * 7
  
  def IsAuthenticated(f: => S => Request[AnyContent] => Result) = {
    authenticated(validateToken, onUnauthorized) { session =>
      Action { request => 
        val userId = userIdFromSession(session)
        val data = dataFromSession(session)
        val t = authenticator.bake(userId, maxIdleTimeInSeconds, sslSessionId(request), data)
        f(session)(request).asInstanceOf[PlainResult].withSession(authenticatonTokenName -> t)
      }
    }
  }

  private def authenticated[A](
    username: RequestHeader => Option[S],
    onUnauthorized: RequestHeader => Result)(action: S => Action[A]): Action[(Action[A], A)] = {

    val authenticatedBodyParser = BodyParser { request =>
      username(request).map { user =>
        val innerAction = action(user)
        innerAction.parser(request).mapDone { body =>
          body.right.map(innerBody => (innerAction, innerBody))
        }
      }.getOrElse {
        Done(Left(onUnauthorized(request)), Input.Empty)
      }
    }

    Action(authenticatedBodyParser) { request =>
      val (innerAction, innerBody) = request.body
      innerAction(request.map(_ => innerBody))
    }

  }
  
  def AuthenticationSuccess(r: Result, concreteSession: S)(implicit req: Request[AnyContent]) = {

    val userId = userIdFromSession(concreteSession)
    
    val t = authenticator.bake(userId, maxIdleTimeInSeconds, sslSessionId(req), "")
    r.asInstanceOf[PlainResult].withSession(req.session + (authenticatonTokenName -> t))
  }
}
