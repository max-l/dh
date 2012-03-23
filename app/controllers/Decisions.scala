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


object Decisions extends BaseDecisionHubController with ConcreteSecured {

  import models._

  val decisionForm = Form(
    mapping(
      "title" -> nonEmptyText, 
      "punchLine"  -> nonEmptyText,
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

  def recordInvitationList = IsAuthenticated { dhSession => implicit request =>

     //format is {"decisionId":"1234","request":"169028456551622","to":["100003662792844"]}

    val js = request.body.asJson.get

    val (requestId, decisionId, reciptientFacebookIds) = 
      try {(
        Util.parseLong((js \ "request").as[String]),
        Util.parseLong((js \ "decisionId").as[String]),
        (js \ "to").as[Seq[String]].map(Util.parseLong(_))
      )}
      catch {
        case e:Exception => logger.error("Bad json format")
        throw e
      }

    logger.info("Invited participants to decision " + decisionId)

    DecisionManager.createParticipations(requestId, decisionId, reciptientFacebookIds)
    Ok
  }

  def submit = IsAuthenticated { dhSession => implicit request =>
    decisionForm.bindFromRequest.fold(
      errors => {
        BadRequest(html.decision(errors)(dhSession))},
      decision => transaction {

        val d0 = decision.copy(ownerId = dhSession.userId)
        val d = models.Schema.decisions.insert(d0)

        Redirect(routes.Decisions.show(d.id))
      })
  }

  def create = IsAuthenticated { dhSession => implicit request =>

    Ok(html.decision(decisionForm.fill(Decision.create))(dhSession))
  }

  def show(id: Long) = IsAuthenticated { dhSession => implicit request => 
    transaction(Schema.decisions.lookup(id)) match {
      case Some(d) => Ok(html.decisionDetailedView(d)(dhSession)) 
      case None    => Redirect(routes.Decisions.myDecisions(dhSession.userId)) 
    }
  }

  def invite(decisionId: Long) = IsAuthenticated { dhSession => implicit request => 
    transaction(Schema.decisions.lookup(decisionId)) match {
      case Some(d) => 
        
        val invitationMessage = "Hi, I have invited you as a voter for this decision : " + d.title

        Ok(html.inviteVoters(d, invitationMessage)(dhSession)) 
      case None    => Redirect(routes.Decisions.myDecisions(dhSession.userId)) 
    }
  }
  
  def edit(id: Long) = IsAuthenticated { dhSession => implicit request => 
    transaction(Schema.decisions.lookup(id)) match {
      case Some(d) => Ok(html.decision(decisionForm.fill(d))(dhSession)) 
      case None    => Redirect(routes.Decisions.myDecisions(dhSession.userId)) 
    }
  }

  val facebookLoginManager = new FacebookOAuthManager(
    "300426153342097", 
    "7fd15f25798be11efb66e698f73b9aa6",
    "http://localhost:9000/fbauth")
  
  def myDecisionz = MaybeAuthenticated { dhSession => implicit request =>

    
    //println(request.body.asText)
    //println(request.body.asJson)
    //println(request.body.asFormUrlEncoded)
    
    dhSession.dhSession match {
      case None =>
        
        val b = request.body.asFormUrlEncoded

        facebookLoginManager.authenticateSignedRequest(b) match {
          case Some(userId) =>
            AuthenticationManager.lookupFacebookUser(userId) match {
              case Some(u) => 
                val ses = new DecisionHubSession(u, request)
                logger.info("registered user" + userId + " authenticated.")

                val requestIds =
                   request.queryString.get("request_ids").
                     flatten.map(parseLong(_))

                DecisionManager.acceptInvitation(requestIds)
                AuthenticationSuccess(Redirect(routes.Application.index), ses)
              case None =>
                logger.info("User" + userId + " not registered.")
                Redirect(routes.Application.login)
            }
          case None => 
            Redirect(routes.Application.login)
        }

      case Some(sess) =>
        showDecisionsOf(sess.userId, sess)
    }
    
    //fb_source=notification&request_ids=330354990346061%2C345752415476609%2C194291397351464%2C153667191422301%2C345240682188366%2C135736843222281&ref=notif&app_request_type=user_to_user&notif_t=app_request
  }
  
  def myDecisions(ownerId: Long) = IsAuthenticated { dhSession => implicit request =>
     showDecisionsOf(ownerId, dhSession)
  }
  
  private def showDecisionsOf(ownerId: Long, sess: DecisionHubSession) = {
    val d = 
      transaction { 
        Schema.decisions.where(_.ownerId === ownerId).toList
      }

    Ok(html.decisionSet("", d)(sess))
  }
}

