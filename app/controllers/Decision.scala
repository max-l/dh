package controllers


import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation._
import play.api.data.validation.Constraints._

import views._

object CreateDecision extends Controller with Secured {

  import models._

  val decisionForm = Form(
    mapping(
      "id" -> of[Long],
      "ownerId" -> of[Long],
      "title" -> nonEmptyText, 
      "punchLine"  -> nonEmptyText,
      "summary" -> optional(text),
      "votesAreAnonymous" -> boolean
    )
    {(id, ownerId, title, punchLine, summary, votesAreAnonymous) =>

       Decision(id, title, punchLine, summary, votesAreAnonymous)
    }
    {decision => 
       import decision._ 
       Some((id, ownerId, title, punchLine, summary, votesAreAnonymous))
    }
  )


}