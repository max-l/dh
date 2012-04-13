package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.validation.Constraints._
import com.strong_links.crypto.ToughCookieBakery
import play.api.libs.ws.WS
import models._
import views._
import org.squeryl.PrimitiveTypeMode._
import org.h2.command.ddl.CreateUser
import play.api.templates.Html
import com.decision_hub.AuthenticationManager
import com.decision_hub._
import com.decision_hub.Util._
import com.decision_hub.FacebookProtocol._


object MainPage extends BaseDecisionHubController {

  
  val LANDING_PAGE_COOKIE_NAME = "landingPathAfterLogin"
  
  
  def externalLogin(provider: String, currentPageUri: String) = MaybeAuthenticated { mpo => implicit req =>
    provider match {
      case "facebook" =>
        Redirect(loginRedirectUrl).withCookies(
          Cookie(LANDING_PAGE_COOKIE_NAME, currentPageUri, maxAge = 2 * 60)
        )
    }
  }
  
  def landingPage(session: Option[DecisionHubSession]) =
    html.fcpe((defaultLandingPage, Screens.decisionSummariesPage(session)), session.map(_.displayName), false)
  
  def index = MaybeAuthenticated { session =>  r =>

    val displayName = 
      for(sess <- session;
          u <- AuthenticationManager.lookupUser(sess.userId))
        yield u.displayableName

    val uri = r.cookies.get(LANDING_PAGE_COOKIE_NAME).map(_.value)

    uri match {
      case None => 
        Ok(landingPage(session))
      case Some(u) => // post login
        Ok(html.fcpe((defaultLandingPage, Html.empty), displayName, true))
    }
  }

  val defaultLandingPage = routes.Screens.decisionSummaries.url
  
  def facebookChannelFile = Action {
    val expire = 60*60*24*365

     Ok("<script src='//connect.facebook.net/en_US/all.js'></script>").withHeaders(
       "Pragma" -> "public", 
       "Cache-Control" -> ("max-age="+ expire),
       "Expires" -> ("max-age=" + expire)
     )
  }
  
  private def extractRequestIds(m: Map[String,Seq[String]], k: String) = 
    for(s <- m.get(k).flatten;
        id <- s.split(',').toSeq)
    yield Util.parseLong(id)


  def clientSideAuthorize = 
    Ok(html.test())
  
  // TODO: ensure that reverse proxy only forwards here when HTTPS
  def facebookCanvasUrl = MaybeAuthenticated(expect[FBClickOnApplication]) { session => implicit request =>

    request.body match {
      case FBClickOnApplicationNonRegistered(js) =>

        extractRequestIds(request.queryString, "request_ids").headOption match {
          case None => 
            Ok(landingPage(session))
          clientSideAuthorize
          case Some(reqId) => Async(
            Dialogs.authorizeAppPage(reqId).map(page => Ok(html.fcpe(page, None, false)))
          )
          clientSideAuthorize
      }
      case FBClickOnApplicationRegistered(fbUserId) =>
        AuthenticationManager.lookupFacebookUser(fbUserId) match {
          case Some(u) =>
            val session = new DecisionHubSession(u, request)
            this.logger.debug("registered user " + fbUserId + " authenticated.")

            //no need to redirect because : (1) we are in an iframe, (2) we are on https
            AuthenticationSuccess(
              Ok(html.fcpe((defaultLandingPage, Screens.decisionSummariesPage(Some(session))), Some(u.displayableName), false)),
              session
            )
            //AuthenticationSuccess(Redirect(routes.MainPage.index), ses)
          case None => // user clicked on 'my applications'

            this.logger.error("non fatal error : fb user " + fbUserId + 
                " registered with FB, but not present in the DB, only explanation : app crash on response from facebook oaut registration.")
            // the login redirect will re import user info... 
            Redirect(routes.Dialogs.login)
        }
    }
  }

  def logout = Action { req =>
    Redirect("http://localhost:9000").withNewSession.flashing(
      "success" -> "You've been logged out"
    ).withCookies(Cookie("DISPLAY_AS_LOGGED_IN","false", maxAge = 0, httpOnly = false))
  }

  def fbauth = Action { implicit req =>
    import FacebookProtocol.facebookOAuthManager._
    val res = 
      for(acCode <- obtainAuthorizationCode(req.queryString).left;
          authToken <- obtainAccessToken(acCode).left;
          info <- obtainMinimalInfo(authToken.value).left) 
      yield info

    res match {
      case Left(info) => transaction {
        val u = AuthenticationManager.authenticateOrCreateUser(info)
        val ses = new DecisionHubSession(u, req)

        AuthenticationSuccess(Redirect(routes.MainPage.index), ses)
      }
      case Right(error) => 
        Logger.info("Failed Logon " + res)
        Redirect(routes.Dialogs.login)
    }
  }  
}