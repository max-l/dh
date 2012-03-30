package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import com.strong_links.crypto.ToughCookieBakery
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import com.strong_links.crypto.ToughCookieStatus
import models.MainPageObjects
import com.decision_hub._
import models._


object Secured {
  
  val authenticatonTokenName = "authenticatonToken"
  
  val authenticator = 
    Play.current.configuration.getString("application.secret").
      map(new ToughCookieBakery(_)).getOrElse(sys.error("missing config 'application.secret'"))

  val sslSessionIdHeaderName = 
    Play.current.configuration.getString("application.sslSessionIdHeaderName")
}

case class DecisionHubSession(userId: Long, dataInCookie: String, requestHeaders: RequestHeader) {
  def this(u: User, r: Request[_]) = this(u.id, u.displayableName, r)

  def displayName = dataInCookie
}

trait ConcreteSecured extends Secured[DecisionHubSession, MainPageObjects] {

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
  
  def mainPageObject(s: Option[DecisionHubSession], h: RequestHeader) = new MainPageObjects(s, h)
}


trait Secured[S,O] {

  import Secured._
  
  
  def loadSession(userId: String, dataInCookie: String, h: RequestHeader): S
  def userIdFromSession(s: S): String
  def dataFromSession(s: S): String
  
  def mainPageObject(s: Option[S], h: RequestHeader): O

  private def validateToken(request: RequestHeader) = 
    for(authenticatonToken <-request.session.get(authenticatonTokenName); 
        userId <- authenticator.validate(authenticatonToken,sslSessionId((request))) match {
          case (ToughCookieStatus.Valid, Some((_, dataInCookie, userIdInCookie))) =>
            Logger("application").debug("Autenticator cookie valid and not expired")
            Some(loadSession(userIdInCookie,dataInCookie, request))
          case (status, _) => 
            Logger("application").debug("Autenticator invalid or expired : " + status)
            None
        }
    )
    yield userId

  protected def sslSessionId(request: RequestHeader) =
    (for(hn <- sslSessionIdHeaderName;
         sid <- request.headers.get(hn))
     yield sid).getOrElse("fwe12342fewf343434")

     
  private def onUnauthorized(request: RequestHeader) = {
    Logger("application").debug("Autentication failed, will redirect.")
    Results.Redirect(routes.Application.login)
  }
    
  val maxIdleTimeInSeconds = 60 * 45
  
  def MaybeAuthenticated(block: O => Request[AnyContent] => Result): Action[AnyContent] =
    Action(BodyParsers.parse.anyContent) { request =>
      validateToken(request) match {
        case Some(sess) => 
          block(mainPageObject(Some(sess), request))(request).asInstanceOf[PlainResult].withSession(authenticatonTokenName -> bake(sess, request))
        case None =>
          block(mainPageObject(None, request))(request)
      }
    }

  def IsAuthenticated(f: => S => Request[AnyContent] => Result) = {
    Logger("application").debug("B4 auth ")
    authenticated(validateToken, onUnauthorized) { session =>
      Action { request => 
        Logger("application").debug("Autentication success.")
        f(session)(request).asInstanceOf[PlainResult].withSession(authenticatonTokenName -> bake(session, request))
      }
    }
  }
  
  private def bake(s: S, req: Request[AnyContent]) = {
    val userId = userIdFromSession(s)
    val data = dataFromSession(s)
    authenticator.bake(userId, maxIdleTimeInSeconds, sslSessionId(req), data)
  }

  private def authenticated[A](
    tokenValidator: RequestHeader => Option[S],
    onUnauthorized: RequestHeader => Result)(action: S => Action[A]): Action[(Action[A], A)] = {

    val authenticatedBodyParser = BodyParser { request =>
      tokenValidator(request).map { user =>
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
    val dataInCoookie = dataFromSession(concreteSession)
    
    val t = authenticator.bake(userId, maxIdleTimeInSeconds, sslSessionId(req), dataInCoookie)
    r.asInstanceOf[PlainResult].withSession(req.session + (authenticatonTokenName -> t))
  }
}
