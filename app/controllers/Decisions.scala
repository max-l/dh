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

object Decisions extends Controller with Secured {

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

  def submit = Action { implicit request =>
    decisionForm.bindFromRequest.fold(
      errors => {

        BadRequest(html.decision(errors))},
      decision => transaction {

        val d = models.Schema.decisions.insert(decision)

        Redirect(routes.Decisions.show(d.id))
      })
  }

  //def decisionEdit(id: Long) = Ok(html.decision(decisionForm.fill(Decision.create)))

  def create = Action {
    Ok(html.decision(decisionForm.fill(Decision.create)))
  }

  def show(id: Long) = Action(transaction {

    val d = models.Schema.decisions.lookup(id).get
    
    Ok(html.decision(decisionForm.fill(d)))
  })
}

