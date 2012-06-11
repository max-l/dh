package com.decision_hub


import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import com.strong_links.crypto.ToughCookieBakery
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import com.strong_links.crypto.ToughCookieStatus
import com.decision_hub._
import models._
import play.api.mvc.PlainResult
import play.api.libs.concurrent.Promise
import controllers.JSonRestApi
import controllers.DecisionHubSession


object Secured {


  val toughCookieBakery = 
    Play.current.configuration.getString("application.secret").
      map(new ToughCookieBakery(_)).getOrElse(sys.error("missing config 'application.secret'"))
}


trait Secured[S] {

  import Secured._
  
  def logger: Logger
  
  def loadSession(userId: String, dataInCookie: String, h: RequestHeader): S
  def userIdFromSession(s: S): String
  def dataFromSession(s: S): String
  def urlForUnauthorized: String
  def authenticatonTokenName: String
  def sslSessionIdHeaderName: Option[String]

  def validateToken(request: RequestHeader) =
    //request.cookies.get("FBAuth") match {
    request.headers.get("FBAuth") match {
      case None => None
      case Some(fbAuth) =>
        val a = com.codahale.jerkson.Json.parse[FBAuthResponse](fbAuth)

        FacebookProtocol.authenticateSignedRequest(a.signedRequest) match {
          case None => Some(Right(""))
          case Some(_) => 
            Some(Left(loadSession(a.userID.toString,"", request)))
        }
    }
  
  protected def sslSessionId(request: RequestHeader) =
    (for(hn <- sslSessionIdHeaderName;
         sid <- request.headers.get(hn))
     yield sid).getOrElse("fwe12342fewf343434")

      
  private def onUnauthorized(request: RequestHeader) = {
    logger.debug("Autentication failed, will redirect.")
    Results.Redirect(urlForUnauthorized)
  }
    
  val maxIdleTimeInSeconds = 45 * 60
  
  val rememberMyExpirationInSeconds = 60 * 60 * 24 * 30
  
  def MaybeAuthenticated(block: Option[S] => Request[AnyContent] => Result): Action[AnyContent] =
    MaybeAuthenticated(BodyParsers.parse.anyContent)(block)
    
  def MaybeAuthenticated[A](bp: BodyParser[A])(block: Option[S] => Request[A] => Result): Action[A] =
    Action(bp) { request =>
      validateToken(request) match {
        case Some(Left(session)) => 
          val result = block(Some(session))(request)
          createOrExtendAuthenticator(request, session, result, false)
        case Some(Right(_)) =>
          //session invalid or expired, we clear the session's content
          val r = (block(None)(request))

          r match {
            case pr:PlainResult => pr.withNewSession
            case ar:AsyncResult =>
            AsyncResult(ar.result.map(res => res.asInstanceOf[PlainResult].withNewSession))
          }
        case None =>
          //no authenticator, we pass through
          block(None)(request)
      }
    }

  def IsAuthenticated(f: => S => Request[AnyContent] => Result):play.api.mvc.Action[(play.api.mvc.Action[AnyContent], AnyContent)] =
    IsAuthenticated(BodyParsers.parse.anyContent)(f)

  def IsAuthenticated[A](bp: BodyParser[A])(f: => S => Request[A] => Result) = {
    logger.debug("B4 auth ")
    authenticated(validateToken, onUnauthorized) { session =>
      Action(bp) { request => 
        logger.debug("Autentication success.")
        val result = f(session)(request)
        createOrExtendAuthenticator(request, session, result, false)
      }
    }
  }  
  
  private def createOrExtendAuthenticator[A](request: Request[A], session: S, r: Result, isCreate: Boolean) = {

    val userId = userIdFromSession(session)
    val encodedAuthenticator = bake(session,request)

    def addCookiesAndSessionData(plainResult: PlainResult) = { 
      val r2 =
        if(isCreate)
          plainResult.withSession(request.session + (authenticatonTokenName -> encodedAuthenticator))
        else // Extend
          plainResult.withSession(authenticatonTokenName -> encodedAuthenticator)

       r2.withCookies(
           Cookie("DISPLAY_AS_LOGGED_IN","true", maxAge = maxIdleTimeInSeconds, httpOnly = false),
           Cookie("REMEMBERED_USER",userId.toString, maxAge = maxIdleTimeInSeconds, httpOnly = false)
          )
    }

    r match {
      case pr:PlainResult =>
        addCookiesAndSessionData(pr)
      case ar:AsyncResult =>
        AsyncResult(ar.result.map(res =>
          addCookiesAndSessionData(res.asInstanceOf[PlainResult])
        ))
    }
  }

  private def bake[A](s: S, req: Request[A]) = {
    val userId = userIdFromSession(s)
    val data = dataFromSession(s)
    toughCookieBakery.bake(userId, maxIdleTimeInSeconds, sslSessionId(req), data)
  }

  private def authenticated[A](
    tokenValidator: RequestHeader => Option[Either[S,_]],
    onUnauthorized: RequestHeader => Result)(action: S => Action[A]): Action[(Action[A], A)] = {

    val authenticatedBodyParser = BodyParser { request =>
      tokenValidator(request) match {
        case Some(Left(user)) => 
          val innerAction = action(user)
          innerAction.parser(request).mapDone { body =>
            body.right.map(innerBody => (innerAction, innerBody))
          }
        case _ =>
          Done(Left(onUnauthorized(request)), Input.Empty)
      }
    }

    Action(authenticatedBodyParser) { request =>
      val (innerAction, innerBody) = request.body
      innerAction(request.map(_ => innerBody))
    }
  }

  def AuthenticationSuccess[A](result: Result, concreteSession: S)(implicit request: Request[A]) = {

    val userId = userIdFromSession(concreteSession)
    val dataInCoookie = dataFromSession(concreteSession)
    val encodedAuthenticator = toughCookieBakery.bake(userId, maxIdleTimeInSeconds, sslSessionId(request), dataInCoookie)
    createOrExtendAuthenticator(request, concreteSession, result, true)
  }
}
