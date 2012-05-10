package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.validation.Constraints._
import com.strong_links.crypto.ToughCookieBakery
import play.api.libs.ws.WS
import models._
import views._
import org.squeryl.PrimitiveTypeMode._
import org.h2.command.ddl.CreateUser
import play.api.templates.Html
import com.decision_hub.AuthenticationManager
import com.decision_hub._
import com.decision_hub.Util._
import com.decision_hub.FacebookProtocol._
import play.mvc.Result




object JSonRestApi extends BaseDecisionHubController {

    
  
  def getDecision(dId: Long) = Action { r =>

    val d =
      inTransaction {
         import Schema._
         decisions.lookup(dId)
      }

    if(d.isEmpty)
      NotFound
    else
      Ok(com.codahale.jerkson.Json.generate(d.get))
  }
}







