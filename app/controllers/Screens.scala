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
        import play.api.libs.json.Json._
        val js = toJson(errorMap : Map[String,String])
        BadRequest(js)
    }
  }  
  
  def decisionDetails(decisionId: Long) = MaybeAuthenticated { session => implicit request =>

    val currentUserId = session.map(_.userId)

    val (decisionSummary, participantDisplays, invitationDisplays, currentUserCanVote, currentUserIsAdmin) =
      DecisionManager.decisionDetails(decisionId, currentUserId)

    Ok(html.decisionDetailedView(decisionSummary, participantDisplays, invitationDisplays, currentUserCanVote, currentUserIsAdmin))
  }

  def participants(decisionId: Long, page: Int, size: Int) = MaybeAuthenticated { mpo => implicit request =>

    val r = DecisionManager.participantAndInvitation(decisionId, page, size)

    Ok(html.participantList(r._1, r._2))
  }
  
  def extractRememberedUserInfo(request: Request[_]) =
    request.cookies.get("REMEMBERED_USER").flatMap { id =>
      try {
        Some(java.lang.Long.parseLong(id.value))
      }
      catch {
        case e: NumberFormatException => None
      }
    }

  def mainScreenDef(session: Option[DecisionHubSession], request: Request[_]) =
    session match {
      case None =>
        extractRememberedUserInfo(request) match {
          case None => html.mainScreen(DecisionManager.decisionSummariesMostActive, Nil)
          case Some(uId) =>
            val ds = DecisionManager.decisionSummariesOf(uId, false)
            html.mainScreen(ds, Nil)
        }
      case Some(s) =>
        val pis = DecisionManager.pendingInvitations(s.userId)
        logger.debug("user %d has %d pending invitations".format(s.userId, pis.size))
        val ds = DecisionManager.decisionSummariesOf(s.userId, false)
        html.mainScreen(ds, pis)
    }

  def mainScreen = MaybeAuthenticated { session => implicit request =>
    Ok(mainScreenDef(session, request))
  }
}

