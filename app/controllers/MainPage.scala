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


case class Todo(id: String, text: String)

object MainPage extends BaseDecisionHubController {

  def home = Action { r =>
    Ok(html.home())
  }

  def newDecision = Action { req =>

    val d = DecisionManager.newDecision

    Redirect("/decision/" + d.id)
  }

  def decision(decisionId: String) = Action { req =>

    if(DecisionManager.decisionExists(decisionId)) 
      Ok(html.adminScreen(decisionId))
    else
      Redirect("/")
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

              Ok(html.voterScreen(
                  true, 
                  false, 
                  appRequestInfoRawJson,
                  generate(Seq(d.id)),
                  "'" + Util.encodeAsJavascript(d.title) + "'"
              ))
            }
          }
      }
      case FBClickOnApplicationRegistered(fbUserId) =>
        val appReqId = extractRequestIds(request.queryString, "request_ids").headOption
        DecisionManager.respondToAppRequestClick(appReqId, fbUserId) match {
          case Some(u) => AuthenticationSuccess(Ok(html.fbVoterScreen()), new DecisionHubSession(u, request))
          // THis case is only possible with failure of POST /recordInvitationList 
          case None => Redirect("/")
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

/*
  def loginWithFacebookToken = MaybeAuthenticated(expectJson[FBAuthResponse]) { session => implicit request =>
    session match {
      case Some(_) => Ok // already logged in...
      case _ => FacebookProtocol.authenticateSignedRequest(request.body.signedRequest) match {
        case None =>
          logger.debug("Invalid FB signedRequest.")
          BadRequest
        case Some(req) =>

          val fbUserId = java.lang.Long.parseLong((req \ "user_id").as[String])

          AuthenticationManager.lookupFacebookUser(fbUserId) match {
            case Some(u) =>
              logger.debug("fb user %s authenticated.".format(fbUserId))
              AuthenticationSuccess(Ok, new DecisionHubSession(u, request))
            case None =>

              val fbCode = (req \ "code").as[String]

              FacebookProtocol.facebookOAuthManager.obtainMinimalInfo(request.body.accessToken) match {
                case Left(info) => transaction {
                  val u = AuthenticationManager.authenticateOrCreateUser(info)
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
*/  

}