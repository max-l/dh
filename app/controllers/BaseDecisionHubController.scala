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

  /**
   * userId in token has precedence over the session's :
   * 
   * The only case where token.userId and session.userId are both not None
   * and different, is when a logged in FB user is accessing a Decision not link
   * to FB (i.e. a public or email based one).
   */
  val userId = 
    token.userId orElse session.map(_.userId)

  //.getOrElse(sys.error("Insuficient access rights"))
  def isOwnerOfDecision = 
    userId.map(_ == decision.ownerId).getOrElse(false)

  lazy val publicGuid =
    if(token.userId.isEmpty)
      token.id
    else inTransaction {
      DecisionManager.getDecisionPublicGuid(decision.id)
    } 
    
  lazy val isParticipant = inTransaction {

    userId.map { uId =>
      Schema.decisionParticipations.where(
        dp => dp.decisionId === decision.id and dp.voterId === uId).headOption.isDefined
    }.getOrElse(false)
  }
  
  def canAdmin = attemptAdmin(Unit).isLeft
  
  def canVote = attemptVote(Unit).isLeft

  def attemptAdmin[A](a: => A):Either[A,String] =
    attemptAdmin((z:Long) => a)

  def attemptVote[A](a: => A):Either[A,String] =
    attemptVote((z:Long) => a)

  def attemptAdmin[A](a: Long => A):Either[A,String] = {

      (decision.mode, session,  userId) match {
        case (Public, _, Some(uId)) if isOwnerOfDecision => Left(a(uId))
        case (Public, _, None   ) => Right("cannot administer this decision with a public link.")
        
        case (EmailAccount, _, Some(uId)) if isOwnerOfDecision => Left(a(uId))
        case (EmailAccount, _, None   ) => Right("cannot administer this decision with a public link.")
        
        case (FBAccount, Some(_), Some(uId)) if isOwnerOfDecision => Left(a(uId))
        case (FBAccount, None, _) => Right("You must be logged in to your facebook account in order to administer this decision")
        case _ => Right("Insufficient rights")
      }
  }
  
  def attemptVote[A](a: Long => A) = 
      (decision.mode, session,  userId) match {
        case (Public, _, Some(uId)) if isParticipant => Left(a(uId))
        case (Public, _, None   ) => Right("cannot vote with a public link.")
        
        case (EmailAccount, _, Some(uId)) if isParticipant => Left(a(uId))
        case (EmailAccount, _, None   ) => Right("cannot vote with a public link.")
        
        case (FBAccount, Some(_), Some(uId)) if isParticipant => Left(a(uId))
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

  override def validateToken(request: RequestHeader) =
    //request.cookies.get("FBAuth") match {
    request.headers.get("FBAuth") match {
      case None => None
      case Some(fbAuth) =>
        val a = com.codahale.jerkson.Json.parse[FBAuthResponse](fbAuth)
        
        JSonRestApi.authenticateFbAuth(a) match {
          case None => Some(Right(""))
          case Some(user) => 
            Some(Left(DecisionHubSession(user.id,"", request)))
        }
    }
  
  def loadSession(userId: String, dataInCookie: String, h: RequestHeader) = sys.error("!!!")
    
  def userIdFromSession(s: DecisionHubSession) = s.userId.toString
  def dataFromSession(s: DecisionHubSession) = s.dataInCookie
}
