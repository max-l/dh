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

  def customParserLong: BodyParser[Long] = null // (RequestHeader) => Iteratee[Array[Byte], Either[Result, Long]] = null
  
  def jsr = Action(customParserLong) { r =>
    
    r.body
    
    Ok
  }
  
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
        BadRequest(html.decision(errors))},
      decision => transaction {

        val d0 = decision.copy(ownerId = dhSession.userId)
        val d = models.Schema.decisions.insert(d0)

        Redirect(routes.Decisions.show(d.id))
      })
  }

  def create = IsAuthenticated { dhSession => implicit request =>

    Ok(html.decision(decisionForm.fill(Decision.create)))
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
      case Some(d) => Ok(html.decision(decisionForm.fill(d))) 
      case None    => Redirect(routes.Decisions.myDecisions(dhSession.userId)) 
    }
  }

  implicit val facebookLoginManager = new FacebookOAuthManager(
    "300426153342097", 
    "7fd15f25798be11efb66e698f73b9aa6",
    "http://localhost:9000/fbauth")

  def myDecisionz = MaybeAuthenticated { dhSession => implicit request =>

    println("--::::::::::::::>")
    //println(request.body.asJson)
    //println(request.body.asFormUrlEncoded)
    val b = request.body.asFormUrlEncoded.get

    import FacebookProtocol._

    FacebookProtocol.authenticateSignedRequest(b).map(_ match {
      case FBClickOnApplicationNonRegistered(_) => 
        Redirect(facebookLoginManager.loginWithFacebookUrl)
      case FBClickOnApplicationRegistered(fbUserId) =>
        AuthenticationManager.lookupFacebookUser(fbUserId) match {
          case Some(u) =>
            val ses = new DecisionHubSession(u, request)
            this.logger.debug("registered user " + fbUserId + " authenticated.")

            val requestIds =
               request.queryString.get("request_ids").
                 flatten.map(Util.parseLong(_))

            DecisionManager.acceptInvitation(requestIds)
            AuthenticationSuccess(Redirect(routes.Application.index), ses)
          case None => // user clicked on 'my applications'
            this.logger.error("non fatal error : fb user " + fbUserId + 
                " registered with FB, but not present in the DB, only explanation : app crash on response from facebook oaut registration.")
            Redirect(routes.Application.login)
        }
    }).getOrElse(BadRequest)
  }
  
  def decisionSummaries = MaybeAuthenticated { dhSession => implicit request =>
    
    val dss = Seq(
      new DecisionSummary(
          1, "Za big Decision", "this is about the most important decision ever", 
          Seq(
            "Za best alternative" ->  454,
            "The most perfect alternatice" ->  142,
            "The alternatice of the enlightened" ->  33
          ),
          67,
          200, 163, 3
          ),
      new DecisionSummary(
          2, "Pizza or Indian ?", "no comments...", 
          Seq(
            "Pizza" ->  5,
            "Indian" ->  5
          ),
          89,
          26, 10, 6),
      new DecisionSummary(
          3, "Elect the master of the universe", "this will have consequences until the end of times !", 
          Nil,
          40,
          234234, 163, 334
          )
      )
    
    Ok(html.decisionSummaries(dss))
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

