package controllers

import play.api._
import play.api.mvc._
import models._
import views._
import play.api.templates.Html
import com.decision_hub._
import com.decision_hub.Util._
import com.decision_hub.FacebookProtocol._
import com.codahale.jerkson.Json._
import html.adminScreen
import html.voterScreen

/**
 * 
 * Participant View :
 *
 *  Tab1: DecisionPublicView
 *  Tab2: BallotView
 *  Tab3: AdminView (if isOwner)
 *
 * Creation Wizard
 *  1: AdminView / DecisionForm  (click next : Invite participants)
 *  2: AdminView / ParticipantTab (click next : Start Voting)
 *  3: BallotView (optional: click : I don't want to vote on this..., click finish)
 *  4: DecisionPublicView
 *  
 */

case class DecisionInvitationInfo(decisionTitle: String, appReqInfo: FBAppRequestInfo)

object MainPage extends BaseDecisionHubController {

  def home = Action { r =>
    Ok(html.home())
  }

  def newDecision = Action { req =>

    val d = DecisionManager.newDecision

    Redirect(routes.MainPage.decisionAdmin(d.id))
  }

  def decisionAdmin(decisionId: String) = Action { req =>

    if(DecisionManager.decisionExists(decisionId)) 
      Ok(html.adminScreen(decisionId))
    else
      Redirect(routes.MainPage.home)
  }

  def voterScreen(decisionId: String) = Action { req =>
    Ok(html.voterScreen(decisionId))
  }

  def facebookCanvasUrl = MaybeAuthenticated(expect[FBClickOnApplication]) { sess => implicit request =>

    request.body match {
      case FBClickOnApplicationNonRegistered(js) =>

        extractRequestIds(request.queryString, "request_ids").headOption match {
          case None => 
            Ok(html.home())
          case Some(fbAppReqId) => Async {
            FacebookProtocol.lookupAppRequestInfoRaw(fbAppReqId).map { t =>
              val (appRequestInfoRawJson, jsonAppReqInfo) = t
              val d = DecisionManager.getDecision(jsonAppReqInfo.data).get

              Ok(html.fbVoterScreen( 
                  Some(DecisionInvitationInfo(d.title, jsonAppReqInfo))
              ))
            }
          }
      }
      case FBClickOnApplicationRegistered(fbUserId) =>
        val appReqId = extractRequestIds(request.queryString, "request_ids").headOption
        DecisionManager.respondToAppRequestClick(appReqId, fbUserId) match {
          case Some(u) => AuthenticationSuccess(Ok(html.fbVoterScreen(None)), new DecisionHubSession(u, request))
          case None => {
            logger.error("Registered FB user not in the DB : " + fbUserId)
            Redirect(routes.MainPage.home)
          }
        }
    }
  }

  def facebookChannelFile = Action {
    val expire = 60*60*24*365

     Ok("<script src='//connect.facebook.net/en_US/all.js'></script>").withHeaders(
       "Pragma" -> "public", 
       "Cache-Control" -> ("max-age="+ expire),
       "Expires" -> ("max-age=" + expire)
     )
  }
  
  private def extractRequestIds(m: Map[String,Seq[String]], k: String) = 
    for(s <- m.get(k).flatten;
        id <- s.split(',').toSeq)
    yield Util.parseLong(id)

}