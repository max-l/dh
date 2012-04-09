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


object Screens extends BaseDecisionHubController {

  import models._

  def create = IsAuthenticated { dhSession => implicit request =>

    Ok(html.decisionForm(Decision(0L,""), true, Nil))
  }

  def submitNewDecision = IsAuthenticated(expectJson[DecisionPost]) { mpo => implicit request =>

    //request.body.validate match {
      //case Right(t) => BadRequest(com.codahale.jerkson.Json.generate(t))
      //case Left(d) =>
        
    println(request.body)

        Ok
    //}
  }
  
  def edit(decisionId: Long) = IsAuthenticated { sess => implicit request =>

    val (d,alts) = DecisionManager.lookupDecisionForEdit(decisionId, sess.userId)
    
    println("alts : " + alts)
    Ok(html.decisionForm(d, false, alts))
  }
  
  def submitDecisionEdit = IsAuthenticated(expectJson[DecisionPost]) { mpo => implicit request =>

    //request.body.validate match {
      //case Right(t) => BadRequest(com.codahale.jerkson.Json.generate(t))
      //case Left(d) =>
        
    println(request.body)

        Ok
    //}
  }  
  
  def decisionDetails(decisionId: Long) = MaybeAuthenticated { mpo => implicit request =>

    val currentUserId = mpo.map(_.userId)
    val (isCurrentUserParticipant, participantLinks, invitationLinks, d) = DecisionManager.decisionDetails(decisionId, currentUserId)
    Ok(html.decisionDetailedView(d, participantLinks, invitationLinks, isCurrentUserParticipant))
  }

  
  def participants(decisionId: Long, page: Int, size: Int) = MaybeAuthenticated { mpo => implicit request =>

    Ok(html.participantList(
        DecisionManager.participants(decisionId, page, size), 
        DecisionManager.invitations(decisionId, page, size)))
  }
  
  def decisionSummaries = MaybeAuthenticated { mpo => implicit request =>
    
    mpo match {
      case None =>

        Ok(html.decisionSummaries(DecisionManager.decisionSummariesMostActive, Nil))
      case Some(s) =>

        val pis = DecisionManager.pendingInvitations(s.userId)
        println("pis" + pis)
        val ds = DecisionManager.decisionSummariesOf(s.userId, false)
        Ok(html.decisionSummaries(DecisionManager.decisionSummariesMostActive, pis))
    }
  }

}

