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
import play.mvc.Result

import com.codahale.jerkson.Json


object JSonRestApi extends BaseDecisionHubController {

  def js[A](a: A) = Ok(Json.generate(a))

  def saveDecision(decisionId: String) = Action(expectJson[Decision]) { r =>

    if(DecisionManager.updateDecision(r.body))
      Ok
    else
      NotFound
  }
  
  def createDecision = Action(BodyParsers.parse.json) { r =>
    
    val title = ((r.body) \ "title").as[String]
    
    val tok = DecisionManager.newDecision(Decision(0L, title), None)
    
    js(Map("id" -> tok.id))
  }

  def getDecision(decisionId: String) = Action { req =>

    DecisionManager.getDecision(decisionId).
      map(js(_)).getOrElse(NotFound)
  }

  def getDecisionPublicView(decisionId: String) = MaybeAuthenticated { session => req =>
    
    //TODO: checl session.map(_.userId) with token
    
    //val tok = session.map( decisionId)
    js(DecisionManager.decisionPubicView(decisionId))
  }

  def getAlternatives(decisionId: String) = Action { r =>

    js(DecisionManager.getAlternatives(decisionId))
  }

  def submitVote(decisionId: String) = IsAuthenticated { session => req =>
    DecisionManager.voteIsComplete(decisionId, session.userId)
    Ok
  }
  
  def createAlternative(decisionId: String) = Action(BodyParsers.parse.json) { r =>
    //TODO: verify if admin
    //TODO: validate title
    val title = ((r.body) \ "title").as[String]
    val a = DecisionManager.createAlternative(decisionId, title)

    js(Map("id" -> a.id))
  }
  
  def createAlternatives(decisionId: String) = Action(BodyParsers.parse.json) { r =>
    //TODO: verify if admin
    //TODO: validate title
    
    val titles = ((r.body) \\ "title")
    val z = titles.map(_.as[String])
    val a = DecisionManager.createAlternatives(decisionId, z)
    val a2 = a.map(_.id)
    js(a2)
  }  

  def updateAlternative(decisionId: String, altId: Long) = Action(BodyParsers.parse.json) { r =>

    val title = ((r.body) \ "title").as[String]
    DecisionManager.updateAlternative(decisionId, altId, title)
    Ok
  }

  def deleteAlternative(decisionId: String, altId: Long) = Action { r =>
    DecisionManager.deleteAlternative(decisionId, altId)
    Ok
  }
  
  def getParticipants(decisionId: String) = Action { r => 
    js(DecisionManager.participants(decisionId, 0, 1000))
  }

  def getBallot(decisionId: String) = MaybeAuthenticated { session => r =>

    js(DecisionManager.getBallot(decisionId))
  }

  def vote(decisionId: String, altId: Long, score: Int) = IsAuthenticated { session => r =>

    DecisionManager.vote(decisionId, altId, session.userId, score)
    Ok
  }
  
  def recordInvitationList = IsAuthenticated(expectJson[FBInvitationRequest]) { session => request =>

    val invitationRequest = request.body

    FacebookParticipantManager.inviteVotersFromFacebook0(session.userId, invitationRequest)

    logger.info("Invited participants to decision " + invitationRequest.decisionId)
    Ok
  }
  
  def myDecisionIds = IsAuthenticated { session => request =>
    js(DecisionManager.decisionIdsOf(session.userId))
  }
  
  def loginWithFacebookToken = MaybeAuthenticated(expectJson[FBAuthResponse]) { session => implicit request =>
    session match {
      case Some(_) => Ok // already logged in...
      case _ => FacebookProtocol.authenticateSignedRequest(request.body.signedRequest) match {
        case None =>
          logger.debug("Invalid FB signedRequest.")
          BadRequest
        case Some(req) =>

          val fbUserId = java.lang.Long.parseLong((req \ "user_id").as[String])

          FacebookParticipantManager.lookupFacebookUser(fbUserId) match {
            case Some(u) =>
              logger.debug("fb user %s authenticated.".format(fbUserId))
              AuthenticationSuccess(Ok, new DecisionHubSession(u, request))
            case None =>

              val fbCode = (req \ "code").as[String]

              FacebookProtocol.facebookOAuthManager.obtainMinimalInfo(request.body.accessToken) match {                
                case Left(info) => transaction {
                  val u = FacebookParticipantManager.authenticateOrCreateUser(info)
                  val ses = new DecisionHubSession(u, request)
                  AuthenticationSuccess(Ok, new DecisionHubSession(u, request))
                }
                case Right(error) => 
                  Logger.info("Failed to get info from facebook" + error)
                  BadRequest
             }
          }
      }
    }
  }  
}


