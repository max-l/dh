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


object Application extends BaseDecisionHubController with ConcreteSecured {

  import views._
  import views.html.helper._

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) if password == "zaza" => true
      case _ => false 
    })
  )

  def facebookChannelFile = Action {
    val expire = 60*60*24*365

     Ok("<script src='//connect.facebook.net/en_US/all.js'></script>").withHeaders(
       "Pragma" -> "public", 
       "Cache-Control" -> ("max-age="+ expire),
       "Expires" -> ("max-age=" + expire)
     )
  }

  def login = MaybeAuthenticated { mpo => implicit request =>
    
    Ok(html.login(facebookOAuthManager.loginWithFacebookUrl)(mpo))
  }

  def logout = Action { req =>
    Redirect("http://localhost:9000").withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  def indexO = MaybeAuthenticated { mpo =>  r =>
    Ok(html.index(mpo))
  }

  def test = MaybeAuthenticated { mpo =>  r =>
    Ok(html.test())
  }
  
  def emptyPage = MaybeAuthenticated { mpo =>  r =>
    Ok("")
  }
  
  
  def index = MaybeAuthenticated { mpo =>  r =>

    val displayName = 
      for(sess <- mpo.dhSession;
          u <- AuthenticationManager.lookupUser(sess.userId))
        yield u.displayableName

    Ok(html.fcpe(new MainPageObject(false, displayName), displayName))
  }
  
  
  def boots = MaybeAuthenticated { mpo =>  r =>
    Ok(html.boots())
  }  

  def showHelloForm = IsAuthenticated { dhSession => implicit request =>
    Ok(html.index(dhSession))
  }

  def fbauth = Action { implicit req =>
    import facebookOAuthManager._
    val res = 
      for(acCode <- obtainAuthorizationCode(req.queryString).left;
          authToken <- obtainAccessToken(acCode).left;
          info <- obtainMinimalInfo(authToken.value).left) 
      yield info

    res match {
      case Left(info) => transaction {
        val u = AuthenticationManager.authenticateOrCreateUser(info)
        val ses = new DecisionHubSession(u, req)
        AuthenticationSuccess(Redirect(routes.Application.index), ses)
      }
      case Right(error) => 
        Logger.info("Failed Logon " + res)
        Redirect(routes.Application.login)
    }
  }

}

