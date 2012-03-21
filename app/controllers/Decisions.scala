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

  def submit = IsAuthenticated { dhSession => implicit request =>
    decisionForm.bindFromRequest.fold(
      errors => {
        BadRequest(html.decision(errors)(dhSession))},
      decision => transaction {

        val d = models.Schema.decisions.insert(decision)

        Redirect(routes.Decisions.show(d.id))
      })
  }

  def create = IsAuthenticated { dhSession => implicit request =>

    Ok(html.decision(decisionForm.fill(Decision.create))(dhSession))
  }

  def show(id: Long) = IsAuthenticated { dhSession => implicit request => 
    transaction {

      models.Schema.decisions.lookup(id)  match {
        case Some(d) => Ok(html.decision(decisionForm.fill(d))(dhSession)) 
        case None    => Redirect(routes.Decisions.myDecisions(dhSession.userId)) 
      }
    }
  }

  def myDecisions(ownerId: Long) = IsAuthenticated { dhSession => implicit request =>
    val d = 
      transaction { 
        models.Schema.decisions.where(_.ownerId === ownerId).toSeq
      }

      Ok(html.decisionSet("", d)(dhSession))
  }
}

