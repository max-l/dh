package controllers

import org.squeryl.PrimitiveTypeMode._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation._
import play.api.data.validation.Constraints._
import views._
import play.api.libs.json.Json
import com.decision_hub._
import com.decision_hub.Util._
import org.jboss.netty.handler.codec.base64.Base64
import java.sql.Timestamp
import play.api.libs.iteratee.Iteratee
import play.api.templates.Html
import com.decision_hub.FacebookProtocol.FBClickOnApplication
import models._

object Dialogs extends BaseDecisionHubController {

  def login = MaybeAuthenticated { mpo => implicit request =>
    Ok(html.login(FacebookProtocol.loginRedirectUrl))
  }


  def authorizeApp(fbAppReqId: Long) = MaybeAuthenticated { mpo => implicit request =>
    
    val (d, u) = DecisionManager.lookupInvitation(fbAppReqId)
    Ok(html.viewInvitationAndAuthorizeApp(d, u, FacebookProtocol.loginRedirectUrl))
  }
  
  def acceptOrDeclineInvitations = IsAuthenticated(expect[Map[Long,Boolean]]) { session => implicit request =>

    DecisionManager.acceptOrDeclineFacebookInvitations(session.userId, request.body)
    Ok
  }
  
  
  def voteScreen(decisionId: Long) = IsAuthenticated { dhSession => implicit request =>

    val (decision, alts) = DecisionManager.voteScreenModel(decisionId, dhSession.userId)

    Ok(html.voteScreen(decision, alts))
  }
  
  def submitVote(decisionId: Long) = IsAuthenticated(expect[Map[Long,Int]]) { dhSession => implicit request =>

    DecisionManager.vote(dhSession.userId, decisionId, request.body)
    Ok
  }

  def create = IsAuthenticated { dhSession => implicit request =>

    Ok(html.decisionForm(Decision(0L,""), true))
  }

  def submitNewDecision = IsAuthenticated(expectJson[DecisionPost]) { mpo => implicit request =>

    request.body.validate match {
      case Right(t) => BadRequest(com.codahale.jerkson.Json.generate(t))
      case Left(d) =>
        

        Ok
    }
  }

  def recordInvitationList = IsAuthenticated(expectJson[FBInvitationRequest]) { mpo => implicit request =>

    val invitationRequest = request.body

    DecisionManager.inviteVotersFromFacebook(mpo.userId, invitationRequest)

    logger.info("Invited participants to decision " + invitationRequest.decisionId)
    Ok
  }
  
}