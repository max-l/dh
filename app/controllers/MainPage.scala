package controllers

import play.api._
import play.api.mvc._
import models._
import views._
import play.api.templates.Html
import com.decision_hub._
import com.decision_hub.Util._
import com.decision_hub.FacebookProtocol._
import org.squeryl.PrimitiveTypeMode._
import com.codahale.jerkson.Json._
import html.recuperateRegisteredUserInfo

/**
 * Decision public view :
 * 
 *  /d/:decisionId
 * 
 * Admin Link :
 *  
 *  /a/:adminUserId
 *  
 * Voter Link :
 *  
 *  /v/:adminUserId
 *  
 */

object MainPage extends BaseDecisionHubController {

  def home = Action { r =>
    Ok(html.home())
  }

/**
  def newDecision = Action { req =>
    val d = DecisionManager.newDecision
    Redirect(routes.MainPage.decisionAdmin(d.id))
  }
*/

  def app(accessGuid: String) = MaybeAuthenticated { session => r =>
    
    val k = accessKey(accessGuid, session)

    k.attemptView(appPage("new ApplicationView('" + accessGuid + "')")).
      fold(identity, identity)
  }
  
  private def appPage(scriptLing: String) =
    Ok(html.app(Html(scriptLing)))
  
  def appVoter(accessGuid: String) = MaybeAuthenticated { session => r =>
    
    val k = accessKey(accessGuid, session)

    DecisionManager.confirmParticipation(k) match {
      case Left(_) => appPage("new ApplicationView('" + accessGuid + "', true)")
      case Right(_) => Unauthorized("Cannot vote with this link")
    }
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
              val d = FacebookParticipantManager.lookupDecisionIdForAccessGuid(jsonAppReqInfo.data)
              Ok(html.fbVoterScreen(Some(DecisionInvitationInfo(d.title, jsonAppReqInfo, jsonAppReqInfo.data)))) 
            }
          }
      }
      case FBClickOnApplicationRegistered(fbUserId) =>
        val appReqId = extractRequestIds(request.queryString, "request_ids").headOption
        FacebookParticipantManager.respondToAppRequestClick(appReqId, fbUserId) match {
          case Some(u) => AuthenticationSuccess(Ok(html.fbVoterScreen(None)), new DecisionHubSession(u, request))
          case None => {
            logger.error("Registered FB user not in the DB : " + fbUserId)
            Redirect(routes.MainPage.home)
          }
        }
    }
  }
  
  def recuperateRegisteredUserInfo = Action {
    Ok(html.recuperateRegisteredUserInfo())
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


/*


{"linkGuids":{"adminGuid":"07ed3qWBr2eos9dk6vEF64","publicGuid":"RZe6BykT1qemi5E_Zaq4a5",
 "guidSignatures":"BrgU92IXfYYIEl4fUfTqhvEtbKy85Ao2Hdu3UtLe/C0="},
 "isPublic":false,
 "fbAuth":{"accessToken":"AAAERPGomtJEBAObgISQW3Nhez6XmOzkZBPO7kudT52wGHQLufQpQA5Y0UeSEM1AfOkH560pZBazOlEVXLoCAw7LbG1Qh4P6oGmzuknxEs9k4hupKND","userID":"100003718310868","expiresIn":6398,"signedRequest":"AYdahvrRdveqErlR3vr6Ui1R_0cjnXW5Hy_rnsHeoJ0.eyJhbGdvcml0aG0iOiJITUFDLVNIQTI1NiIsImNvZGUiOiIyLkFRRHJjNkh6a0JFOFBVYnYuMzYwMC4xMzM5MTc4NDAwLjUtMTAwMDAzNzE4MzEwODY4fEI2S2VnMVFuRlp3RWxxY2dFdGJWMFlOaWtfTSIsImlzc3VlZF9hdCI6MTMzOTE3MjAwMiwidXNlcl9pZCI6IjEwMDAwMzcxODMxMDg2OCJ9"},
 "title":"zozo",
 "choices":[{"title":"1"},{"title":"2"},{"title":"3"}]
 }



*/