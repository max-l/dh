package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.libs.ws.WS
import models._
import views._
import org.squeryl.PrimitiveTypeMode._
import org.h2.command.ddl.CreateUser
import play.api.templates.Html
import com.decision_hub._
import com.decision_hub.Util._
import com.decision_hub.FacebookProtocol._
import com.codahale.jerkson.Json
import play.api.libs.concurrent.Promise
import com.strong_links.crypto._
import play.api.mvc.Results.EmptyContent


object JSonRestApi extends BaseDecisionHubController {
  
  import Util._
  import CryptoUtil._

  def js[A](a: A) = Ok(Json.generate(a))
  
  
  
  lazy val serverSecret = 
    Play.current.configuration.getString("application.secret").
      map(s =>  s : CryptoField).getOrElse(sys.error("missing config 'application.secret'"))
      
  def newSignedGuidJson = Action {
    js(newSignedGuid(serverSecret))
  }
  
  def newSignedGuid(key: CryptoField) = {
    
    
    val guid1 = Util.newGuid
    val guid2 = Util.newGuid
    val signature = hmacSha256(guid1, guid2)(key)
    
    Guids(guid1, guid2, signature.value)
  }
  
  private def hasValidGuid(cd: CreateDecision) = {
    
    val computedSignature = hmacSha256(cd.linkGuids.adminGuid, cd.linkGuids.publicGuid)(serverSecret)
    
    computedSignature matches cd.linkGuids.guidSignatures
  }
  
  def saveDecision(accessGuid: String) = MaybeAuthenticated(expectJson[DecisionM]) { session => r =>
    
    val k = accessKey(accessGuid, session)

    doIt(DecisionManager.updateDecision(k, r.body))(
      z => if(z) Ok else NotFound
    )
  }
  
  def createDecision = Action(expectJson[CreateDecision]) { r =>
    
    val cd = r.body

    val userAndMode: Either[(User,DecisionPrivacyMode.Value), String] = 
      if(! hasValidGuid(cd)) 
        Right("InvalidGuid")
      else (cd.fbAuth, cd.ownerEmail, cd.ownerName) match {
        case (Some(fbAuth), None, None) => 
          authenticateFbAuth(fbAuth) match {
            case Some(user) => Left((user, DecisionPrivacyMode.FBAccount)) 
            case None => Right("Failed FB info retrieval")
          }
        case (None, Some(ownerEmail), ownerName) =>
          EmailParticipantManager.lookupUserByEmail(ownerEmail) match {
            case Some(user) =>
              Left((user, DecisionPrivacyMode.EmailAccount))
            case None =>
              Left((User(email = Some(ownerEmail), firstName = ownerName, confirmed = false), DecisionPrivacyMode.EmailAccount))
          }
        case (None, None, ownerName@Some(_)) =>
          Left((User(nickName = ownerName)), DecisionPrivacyMode.Public)
        case _ => Right("Invalid information to create decision " + cd)
      }

    userAndMode.fold( 
      left => {  
        val (user, mode) = left
        DecisionManager.newDecision(cd, user, mode)
        js(mode.toString)
      },
      errorMsg => {
        logger.error(errorMsg)
        BadRequest
      }
    )
  }
  
  def inviteByEmail(accessGuid: String) = MaybeAuthenticated(expectJson[Seq[String]]) { session => req =>
    val k = accessKey(accessGuid, session)
    
    doIt(DecisionManager.createEmailParticipantsAndSentInvites(k, req.body.toSet))(z => Ok)
  }
  
  def requestEnableEmailInvites(accessGuid: String) = MaybeAuthenticated { session => req =>
    
    val k = accessKey(accessGuid, session)
    Ok
  }

  def getDecision(accessGuid: String) = MaybeAuthenticated { session => req =>

    val k = accessKey(accessGuid, session)

    doIt(DecisionManager.getDecision(k))(js(_))
  }

  def getDecisionPublicView(accessGuid: String) = MaybeAuthenticated { session => req =>
    
    val k = accessKey(accessGuid, session)

    doIt(DecisionManager.decisionPubicView(k))(js(_))
  }

  def getAlternatives(accessGuid: String) = MaybeAuthenticated { session => r =>
    
    val k = accessKey(accessGuid, session)

    doIt(DecisionManager.getAlternatives(k))(js(_))
  }

  def submitVote(accessGuid: String) = MaybeAuthenticated { session => req =>
    
    val k = accessKey(accessGuid, session)
    
    doIt(DecisionManager.voteIsComplete(k))(z => Ok)
  }
  
  def createAlternative(accessGuid: String) = MaybeAuthenticated(BodyParsers.parse.json) { session => r =>
    
    val k = accessKey(accessGuid, session)
    
    val title = ((r.body) \ "title").as[String]
    doIt(DecisionManager.createAlternative(k, title))(a => 
      js(Map("id" -> a.id))
    )
  }

  def updateAlternative(accessGuid: String, altId: Long) = MaybeAuthenticated(BodyParsers.parse.json) { session => r =>

    val k = accessKey(accessGuid, session)
    
    val title = ((r.body) \ "title").as[String]
    
    doIt(DecisionManager.updateAlternative(k, altId, title))(z => Ok)
  }

  def deleteAlternative(accessGuid: String, altId: Long) = MaybeAuthenticated { session => r =>

    val k = accessKey(accessGuid, session)

    doIt(DecisionManager.deleteAlternative(k, altId))(z => Ok)
  }
  
  def getParticipants(accessGuid: String) = MaybeAuthenticated { session => r =>
    
    val k = accessKey(accessGuid, session)
    
    doIt(DecisionManager.participants(k, 0, 1000))(js(_))
  }

  def getBallot(accessGuid: String) = MaybeAuthenticated { session => r =>
    
    val k = accessKey(accessGuid, session)

    doIt(DecisionManager.getBallot(k))(js(_))
  }

  def vote(accessGuid: String, altId: Long, score: Int) = MaybeAuthenticated { session => r =>

    val k = accessKey(accessGuid, session)
    
    doIt(DecisionManager.vote(k, altId, score))(z => Ok)
  }
  
  def recordInvitationList(accessGuid: String) = Action(expectJson[FBInvitationRequest]) { request =>

    val invitationRequest = request.body
    
    authenticateFbAuth(invitationRequest.fbAuthResponse) match {
       case None => BadRequest
       case Some(u) =>
          val ses = new DecisionHubSession(u, request)
          val k = accessKey(accessGuid, ses)
          doIt(FacebookParticipantManager.inviteVotersFromFacebook(k, invitationRequest))(z => {
            logger.info("Invited participants to decision " + k.decision.id)
            Ok
          })
    }
  }
  
  def myDecisionIds = IsAuthenticated { session => request =>
    
    js(DecisionManager.decisionIdsOf(session.userId))
  }
  
  def loginWithFacebookToken = MaybeAuthenticated(expectJson[FBAuthResponse]) { session => implicit request =>
    session match {
      case Some(_) => Ok // already logged in...
      case _ =>
        authenticateFbAuth(request.body) match {
          case Some(u) =>  AuthenticationSuccess(Ok, new DecisionHubSession(u, request))
          case None => BadRequest
        }
    }
  }

  def authenticateFbAuth(fbAuth: FBAuthResponse) = 
      FacebookProtocol.authenticateSignedRequest(fbAuth.signedRequest) match {
        case None =>
          logger.debug("Invalid FB signedRequest.")
          None
        case Some(req) =>

          val fbUserId = java.lang.Long.parseLong((req \ "user_id").as[String])

          FacebookParticipantManager.lookupFacebookUser(fbUserId) match {
            case Some(u) =>
              logger.debug("fb user %s authenticated.".format(fbUserId))
              Some(u)
            case None =>

              val fbCode = (req \ "code").as[String]

              FacebookProtocol.facebookOAuthManager.obtainMinimalInfo(fbAuth.accessToken) match {                
                case Left(info) => transaction {
                  val u = FacebookParticipantManager.authenticateOrCreateUser(info)
                  Some(u)
                }
                case Right(error) => 
                  Logger.info("Failed to get info from facebook" + error)
                  None
             }
          }
      }
}


