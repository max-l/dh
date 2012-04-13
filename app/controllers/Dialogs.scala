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

  
  def authorizeAppPage(fbAppReqId: Long) =
    FacebookProtocol.looupAppRequestInfo(fbAppReqId).map { appReqInfo =>
      
      val d = DecisionManager.lookupDecision(appReqInfo.data)
      
      (routes.Dialogs.authorizeApp(fbAppReqId).url,
       html.viewInvitationAndAuthorizeApp(d, appReqInfo, FacebookProtocol.loginRedirectUrl))
    }

  def authorizeApp(fbAppReqId: Long) = MaybeAuthenticated { mpo => implicit request =>
    Async {
      authorizeAppPage(fbAppReqId).map(r => Ok(r._2))
    }
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

  def recordInvitationList = IsAuthenticated(expectJson[FBInvitationRequest]) { mpo => implicit request =>

    val invitationRequest = request.body

    DecisionManager.inviteVotersFromFacebook(mpo.userId, invitationRequest)

    logger.info("Invited participants to decision " + invitationRequest.decisionId)
    Ok
  }
  
}