package controllers

import models._
import play.api.mvc._
import play.api.Logger
import com.decision_hub._
import play.api.Play
import org.squeryl.PrimitiveTypeMode._


case class DecisionHubSession(userId: Long, dataInCookie: String, requestHeaders: RequestHeader) {
  def this(u: User, r: Request[_]) = this(u.id, "", r)

}


class AccessKey(token: PToken, val decision: Decision, val session: Option[controllers.DecisionHubSession]) {
  import DecisionPrivacyMode._
  
  def accessGuid = token.id
  
  lazy val userId = 
    (token.userId orElse session.map(_.userId)).getOrElse(sys.error("Insuficient access rights"))
  
  private def isOwner = 
    token.userId.map(_ == decision.ownerId).getOrElse(false)
  
  lazy val isParticipant = inTransaction {
    //userId in token has precedence over the session's :
    val userId = token.userId orElse session.map(_.userId)
    
    userId.map { uId =>
      Schema.decisionParticipations.where(
        dp => dp.decisionId === decision.id and dp.voterId === uId).headOption.isDefined
    }.getOrElse(false)
  }

  def attemptAdmin[A](a: => A) = 
      (decision.mode, session,  token.userId) match {
        case (Public, _, Some(_)) if isOwner => Left(a)
        case (Public, _, None   ) => Right("cannot administer this decision with a public link.")
        
        case (EmailAccount, _, Some(_)) if isOwner =>
          if(token.confirmed) Left(a)
          else Right("You must follow the link sent to you email account in order to administer this decision.")
        case (EmailAccount, _, None   ) => Right("cannot administer this decision with a public link.")
        
        case (FBAccount, Some(_), _) if isOwner => Left(a)
        case (FBAccount, None, _) => Right("You must be logged in to your facebook account in order to administer this decision")
        case _ => Right("Insufficient rights")
      }
  
  
  def attemptVote[A](a: => A) = 
      (decision.mode, session,  token.userId) match {
        case (Public, _, Some(_)) if isParticipant => Left(a)
        case (Public, _, None   ) => Right("cannot vote with a public link.")
        
        case (EmailAccount, _, Some(_)) if isParticipant => Left(a)
        case (EmailAccount, _, None   ) => Right("cannot vote with a public link.")
        
        case (FBAccount, Some(_), _) if isParticipant => Left(a)
        case (FBAccount, None, _) => Right("You must be logged in to your facebook account to vote on this decision")
        case _ => Right("Insufficient rights")
      }
  
  def attemptView[A](a: => A) = Left(a)
}

object BaseDecisionHubController extends BaseDecisionHubController

trait BaseDecisionHubController extends Controller with Secured[DecisionHubSession] {

  def accessKey(guid: String, session: controllers.DecisionHubSession): AccessKey = 
    accessKey(guid, Some(session))
  
  def accessKey(guid: String, session: Option[controllers.DecisionHubSession]): AccessKey = inTransaction {
    
    val tok = Schema.pTokens.lookup(guid).get
    val decision = Schema.decisions.lookup(tok.decisionId).get
    new AccessKey(tok, decision, session)
  }

  protected def doIt[A](e: Either[A,String])(f: A => play.api.mvc.Result) = e.fold(
    a => f(a),
    msg => { 
      logger.warn(msg)
      NotFound
    }
  )
  
  val authenticatonTokenName = "authenticatonToken"
      
  val sslSessionIdHeaderName = 
    Play.current.configuration.getString("application.sslSessionIdHeaderName")
    
    
  def urlForUnauthorized = "/"
  
  implicit def logger = Logger("application")

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
