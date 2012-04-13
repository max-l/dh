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

  def submitNewDecision = IsAuthenticated(expectJson[DecisionPost]) { session => implicit request =>

    println(request.body)
    DecisionManager.createNewDecision(session.userId, request.body) match {
      case Left(dId) => Ok(""+dId)
      case Right(js) => BadRequest(com.codahale.jerkson.Json.generate(js))
    }
  }
  
  def edit(decisionId: Long) = IsAuthenticated { sess => implicit request =>

    val (d,alts) = DecisionManager.lookupDecisionForEdit(decisionId, sess.userId)
    
    println("alts : " + alts)
    Ok(html.decisionForm(d, false, alts))
  }
  
  def submitDecisionEdit = IsAuthenticated(expectJson[DecisionPost]) { session => implicit request =>

    DecisionManager.editDecision(session.userId, request.body) match {
      case None => Ok      
      case Some(errorMap) =>
        //case Some(js) => BadRequest(com.codahale.jerkson.Json.generate(js))
        //import play.libs.Json._
        import play.api.libs.json.Json._
        //import play.api.libs.json.Writes._
        //play.api.libs.json.Writes.
        val js0 =  toJson(Map("status" -> "KO", "message" -> "Missing parameter [name]"))
        val js = toJson(errorMap : Map[String,String])
        BadRequest(js)
        //BadRequest(toJson(js))
    }
  }  
  
  def decisionDetails(decisionId: Long) = MaybeAuthenticated { session => implicit request =>

    val currentUserId = session.map(_.userId)

    val (decisionSummary, participantDisplays, invitationDisplays, currentUserCanVote, currentUserIsAdmin) =
      DecisionManager.decisionDetails(decisionId, currentUserId)

    Ok(html.decisionDetailedView(decisionSummary, participantDisplays, invitationDisplays, currentUserCanVote, currentUserIsAdmin))
  }

  
  def participants(decisionId: Long, page: Int, size: Int) = MaybeAuthenticated { mpo => implicit request =>

    Ok(html.participantList(
        DecisionManager.participants(decisionId, page, size), 
        DecisionManager.invitations(decisionId, page, size)))
  }
  
  def decisionSummariesPage(session: Option[DecisionHubSession]) =
    session match {
      case None =>
        
        

        html.decisionSummaries(DecisionManager.decisionSummariesMostActive, Nil)
      case Some(s) =>

        val pis = DecisionManager.pendingInvitations(s.userId)
        println("pis" + pis)
        val ds = DecisionManager.decisionSummariesOf(s.userId, false)
        html.decisionSummaries(DecisionManager.decisionSummariesMostActive, pis)
    }    
  
  def decisionSummaries = MaybeAuthenticated { session => implicit request =>
    Ok(decisionSummariesPage(session))
  }

}

