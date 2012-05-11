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

import com.codahale.jerkson.Json



object JSonRestApi extends BaseDecisionHubController {

  def getDecision(dId: Long) = Action {

    DecisionManager.getDecision(dId).
     map(Json.generate(_)).map(Ok(_)).getOrElse(NotFound)
  }
  
  def getAlternative(dId: Long) = Action { r =>
    println(r.body)
    
    Ok(Json.generate(DecisionManager.getAlternatives(dId)))
  }

  def createAlternative(id: Long) = Action(BodyParsers.parse.json) { r =>
    println(r.body)
    Ok
  }

  def updateAlternative(id: Long, altId: Long) = Action(BodyParsers.parse.json) { r =>
    println(r.body)
    Ok
  }

  def deleteAlternative(id: Long, altId: Long) = Action { r =>
    println(r.body)
    Ok
  }

  //def getBallot(did: Long) = IsAuthenticated { session => r =>
  def getBallot(did: Long) = Action { r =>

    Ok
  }

  //def vote(did: Long, altId: Long, score: Int) = IsAuthenticated { session => r =>
  def vote(did: Long, altId: Long, score: Int) = Action { r =>

    Ok
  }
}







