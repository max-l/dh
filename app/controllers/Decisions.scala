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


object Decisions extends BaseDecisionHubController with ConcreteSecured {

  import models._

  val decisionForm = Form(
    mapping(
      "title" -> nonEmptyText, 
      "punchLine"  -> optional(text),
      "summary" -> optional(text),
      "votesAreAnonymous" -> boolean
    )
    {(title, punchLine, summary, votesAreAnonymous) =>

      val currentUserId = 0L
      Decision(currentUserId, title, punchLine, summary, votesAreAnonymous)
    }
    {decision => 
       import decision._ 
       Some((title, punchLine, summary, votesAreAnonymous))
    }
  )

  def recordInvitationList = IsAuthenticated(expectJson[FBInvitationRequest]) { mpo => implicit request =>

    val invitationRequest = request.body

    DecisionManager.inviteVotersFromFacebook(mpo.userId, invitationRequest)

    logger.info("Invited participants to decision " + invitationRequest.decisionId)
    Ok
  }

  def decisionDetails(decisionId: Long) = MaybeAuthenticated { mpo => implicit request =>

    val currentUserId = mpo.dhSession.map(_.userId)
    val (isCurrentUserParticipant, participantLinks, invitationLinks, d) = DecisionManager.decisionDetails(decisionId, currentUserId)
    Ok(html.decisionDetailedView(d, participantLinks, invitationLinks, isCurrentUserParticipant))
  }

  def create = IsAuthenticated { dhSession => implicit request =>

    Ok(html.decisionForm(Decision(0L,"")))
  }
  
  def participants(decisionId: Long, page: Int, size: Int) = MaybeAuthenticated { mpo => implicit request =>

    Ok(html.participantList(
        DecisionManager.participants(decisionId, page, size), 
        DecisionManager.invitations(decisionId, page, size)))
  }
  
  def viewInvitationAndAuthorizeApp(fbAppReqId: Long) = MaybeAuthenticated { mpo => implicit request =>
    
    val (d, u) = DecisionManager.lookupInvitation(fbAppReqId)
    Ok(html.viewInvitationAndAuthorizeApp(d, u, facebookOAuthManager.loginWithFacebookUrl))
  }
  
  def acceptOrDeclineInvitations = IsAuthenticated(expect[Map[Long,Boolean]]) { session => implicit request =>
    
    println("======>" + request.body)
    
    DecisionManager.acceptOrDeclineFacebookInvitations(session.userId, request.body)
    Ok
  }

  def facebookCanvasUrl = MaybeAuthenticated { dhSession => implicit request =>

    println("--::::::::::::::>")
    //println(request.body.asJson)
    //println(request.body.asFormUrlEncoded)
    val b = request.body.asFormUrlEncoded.get

    import FacebookProtocol._

    FacebookProtocol.authenticateSignedRequest(b).map(_ match {
      case FBClickOnApplicationNonRegistered(js) =>
        val requestIds = request.queryString.get("request_ids").flatten.map(Util.parseLong(_)).head

        Ok(html.fcpe(routes.Decisions.viewInvitationAndAuthorizeApp(requestIds).url, None))
      case FBClickOnApplicationRegistered(fbUserId) =>
        AuthenticationManager.lookupFacebookUser(fbUserId) match {
          case Some(u) =>
            val ses = new DecisionHubSession(u, request)
            this.logger.debug("registered user " + fbUserId + " authenticated.")

            //val requestIds =
            //   request.queryString.get("request_ids").
            //     flatten.map(Util.parseLong(_))

            //DecisionManager.acceptFacebookInvitation(u.id, requestIds)
            AuthenticationSuccess(Redirect(routes.Application.index), ses)
          case None => // user clicked on 'my applications'
            println(";>>>>>>5>")
            this.logger.error("non fatal error : fb user " + fbUserId + 
                " registered with FB, but not present in the DB, only explanation : app crash on response from facebook oaut registration.")
            // the login redirect will re import user info... 
            Redirect(routes.Application.login)
        }
    }).getOrElse{ BadRequest}
  }
  
  def decisionSummaries = MaybeAuthenticated { mpo => implicit request =>
    
    mpo.dhSession match {
      case None =>

        Ok(html.decisionSummaries(DecisionManager.decisionSummariesMostActive, Nil))
      case Some(s) =>

        val pis = DecisionManager.pendingInvitations(s.userId)
        println("pis" + pis)
        val ds = DecisionManager.decisionSummariesOf(s.userId, false)
        Ok(html.decisionSummaries(DecisionManager.decisionSummariesMostActive, pis))
    }
  }

  def voteScreen(decisionId: Long) = IsAuthenticated { dhSession => implicit request =>

    val (decision, alts) = DecisionManager.voteScreenModel(decisionId, dhSession.userId)

    Ok(html.voteScreen(decision, alts))
  }
  

  def submitVote(decisionId: Long) = IsAuthenticated(expect[Map[Long,Int]]) { dhSession => implicit request =>

    DecisionManager.vote(dhSession.userId, decisionId, request.body)
    Ok("Vote recorded !")
  }
}

