package controllers

import models._
import play.api.mvc._
import play.api.Logger
import com.decision_hub._
import play.api.Play



case class DecisionHubSession(userId: Long, dataInCookie: String, requestHeaders: RequestHeader) {
  def this(u: User, r: Request[_]) = this(u.id, u.displayableName, r)

  def displayName = dataInCookie
}

trait BaseDecisionHubController extends Controller with Secured[DecisionHubSession] {

  val authenticatonTokenName = "authenticatonToken"
      
  val sslSessionIdHeaderName = 
    Play.current.configuration.getString("application.sslSessionIdHeaderName")
    
    
  implicit def logger = Logger("application")

  def urlForUnauthorized: String = controllers.routes.Dialogs.login.url
  
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
