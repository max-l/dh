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

  private def validateToken(request: RequestHeader) = 
    for(authenticatonToken <-request.session.get(authenticatonTokenName); 
      userId <- toughCookieBakery.validate(authenticatonToken,sslSessionId((request))) match {
        case (ToughCookieStatus.Valid, Some((_, dataInCookie, userIdInCookie))) =>
          logger.debug("Autenticator cookie valid, userId:" + userIdInCookie)
          Some(loadSession(userIdInCookie,dataInCookie, request))
        case (status, _) => 
          logger.debug("Autenticator invalid or expired : " + status)
          None
      }
    )
    yield userId

  protected def sslSessionId(request: RequestHeader) =
    (for(hn <- sslSessionIdHeaderName;
         sid <- request.headers.get(hn))
     yield sid).getOrElse("fwe12342fewf343434")

      
  private def onUnauthorized(request: RequestHeader) = {
    logger.debug("Autentication failed, will redirect.")
    Results.Redirect(urlForUnauthorized)
  }
    
  val maxIdleTimeInSeconds = 45 * 60
  
  def MaybeAuthenticated(block: Option[S] => Request[AnyContent] => Result): Action[AnyContent] =
    MaybeAuthenticated(BodyParsers.parse.anyContent)(block)
    
  def MaybeAuthenticated[A](bp: BodyParser[A])(block: Option[S] => Request[A] => Result): Action[A] =
    Action(bp) { request =>
      validateToken(request) match {
        case Some(session) => 
          val result = block(Some(session))(request)
          createOrExtendAuthenticator(request, bake(session, request), result, false)
        case None =>
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
        createOrExtendAuthenticator(request, bake(session, request), result, false)
      }
    }
  }  
  
  private def createOrExtendAuthenticator[A](request: Request[A], encodedAuthenticator: String, r: Result, isCreate: Boolean) = {
    
    val plainResult = r.asInstanceOf[PlainResult]
    
    val r2 =
      if(isCreate)
        plainResult.withSession(request.session + (authenticatonTokenName -> encodedAuthenticator))
      else // Extend
        plainResult.withSession(authenticatonTokenName -> encodedAuthenticator)

     r2.withCookies(Cookie("DISPLAY_AS_LOGGED_IN","true", maxAge = maxIdleTimeInSeconds, httpOnly = false)) 
  } 
  
  private def bake[A](s: S, req: Request[A]) = {
    val userId = userIdFromSession(s)
    val data = dataFromSession(s)
    toughCookieBakery.bake(userId, maxIdleTimeInSeconds, sslSessionId(req), data)
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

  def AuthenticationSuccess[A](result: Result, concreteSession: S)(implicit request: Request[A]) = {

    val userId = userIdFromSession(concreteSession)
    val dataInCoookie = dataFromSession(concreteSession)
    val encodedAuthenticator = toughCookieBakery.bake(userId, maxIdleTimeInSeconds, sslSessionId(request), dataInCoookie)
    createOrExtendAuthenticator(request, encodedAuthenticator, result, true)
  }
}
