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

//case class Alternative(title: String)

object JSonRestApi extends BaseDecisionHubController {

  def js[A](a: A) = Ok(Json.generate(a))


  //def saveDecision(decisionId: String) = Action(BodyParsers.parse.json) { r =>
  def saveDecision(decisionId: String) = Action(expectJson[Decision]) { r =>
    
    
    val d = r.body
    println(d.id)
    println(d)
    println(">>>>>>>>>"+d.endsOn)

    if(DecisionManager.updateDecision(r.body))
      Ok
    else
      NotFound
  }

  def getDecision(decisionId: String) = Action { req =>

    println("<<<<<<<<<")
    println(Json.generate(DecisionManager.getDecision(decisionId)))
    println(">>>>>>>>>")
    
    DecisionManager.getDecision(decisionId).
      map(js(_)).getOrElse(NotFound)
  }

  def getAlternatives(decisionId: String) = Action { r =>
    println(r.body)

    js(DecisionManager.getAlternatives(decisionId))
  }

  def createAlternative(decisionId: String) = Action(BodyParsers.parse.json) { r =>
    println(r.body)
    //TODO: verify if admin
    //TODO: validate title
    val title = ((r.body) \ "title").as[String]
    val a = DecisionManager.createAlternative(decisionId, title)

    js(a.id)
  }

  def updateAlternative(decisionId: String, altId: Long) = Action(BodyParsers.parse.json) { r =>
    println("UPDATE : " + r.body)
    //TODO: verify if admin
    val title = ((r.body) \ "title").as[String]
    DecisionManager.updateAlternative(decisionId, altId, title)
    Ok
  }

  def deleteAlternative(decisionId: String, altId: Long) = Action { r =>
    println(r.body)
    DecisionManager.deleteAlternative(decisionId, altId)
    Ok
  }

  //def getBallot(decisionId: String) = IsAuthenticated { session => r =>
  def getBallot(decisionId: String) = Action { r =>

    js(DecisionManager.getBallot(decisionId, 4))
  }

  //def vote(decisionId: String, altId: Long, score: Int) = IsAuthenticated { session => r =>
  def vote(decisionId: String, altId: Long, score: Int) = Action { r =>
    
    DecisionManager.vote(decisionId, altId, 4, score)
    Ok
  }
}







