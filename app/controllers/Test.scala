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
import org.squeryl.PrimitiveTypeMode._
import org.h2.command.ddl.CreateUser
import play.api.templates.Html
import com.decision_hub.AuthenticationManager


object Test extends BaseDecisionHubController {

  import views._
  import views.html.helper._


  def compareVotingMethods = Action {
    Ok(html.compareVotingMethods())
  }
  

  def test = MaybeAuthenticated { mpo =>  r =>
    Ok(html.test())
  }
  
  def emptyPage = MaybeAuthenticated { mpo =>  r =>
    Ok("")
  }

}
