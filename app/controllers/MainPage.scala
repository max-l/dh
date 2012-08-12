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

import models.ev._

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

  private def lang(req: Request[_]) =
      req.cookies.get("CHOSEN_LANGUAGE").map(_.value).getOrElse {
      
        req.headers.get("ACCEPT-LANGUAGE") match {
          case None => "en"
          case Some(l) => 
            l.split(',').map { l =>
              if(l.startsWith("en")) "en"
              else if(l.startsWith("fr")) "fr"
              else  "en"
            }.headOption.getOrElse("en")
        }
      }

  def equivoteRoot = Action { req =>
    Redirect("/" + lang(req))
  }

  def equivoteHome(langCode: String) = 
    equivotePage(langCode, "/")
  
  
  def equivotePage(langCode: String, page: String) = Action { req =>
    
    val l = Languages(langCode match {
      case "" => lang(req)
      case _ => langCode
    })
    
    val pageLink = 
      if(page == "/") "" else ("/" + page)

    Ok( page match {
      case "/" => html.ev.index(pageLink)(l)
      case "proportionalScoreVoting" => html.ev.proportionalScoreVoting(pageLink)(l)
      case "apropos" => html.ev.apropos(pageLink)(l)
      case "simpliquer" => html.ev.simpliquer(pageLink)(l)
    }).withCookies(choosenLangCookie(l.code))
  }

  def choosenLangCookie(code: String) =
    Cookie("CHOSEN_LANGUAGE",code, maxAge = 60 * 60 * 24 * 265)
/*  
  def equivoteFR = Action {
    Ok(html.ev.index(Languages("fr"))).withCookies(choosenLangCookie("fr"))
  }

  def equivoteEN = Action {
    Ok(html.ev.index(Languages("en"))).withCookies(choosenLangCookie("en"))
  }
  
  def proportionalScoreVoting = Action { req =>
    val langCode = lang(req)
    println("===========>" + langCode)
    Ok(html.ev.proportionalScoreVoting(Languages(langCode))).withCookies(choosenLangCookie(langCode))
  }

  def proportionalScoreVotingz(langCode: String) = Action { req =>
    println("zzzzzzzzzzzzzzz>" + langCode)
    Ok(html.ev.proportionalScoreVoting(Languages(langCode))).withCookies(choosenLangCookie(langCode))
  }
*/  
  def cssExp = Action {
    Ok(html.ev.cssExp())
  }
  
  def home = MaybeAuthenticated { s => r =>
    Ok(html.home())
  }
  
  def logout = Action { r =>
    Redirect("/").withNewSession
  }

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

