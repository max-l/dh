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


object Application extends BaseDecisionHubController with ConcreteSecured {

  val facebookLoginManager = new FacebookOAuthManager(
    "300426153342097", 
    "7fd15f25798be11efb66e698f73b9aa6",
    "http://localhost:9000/fbauth")

  
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
    
    Ok(html.login(facebookLoginManager.loginWithFacebookUrl)(mpo))
  }

  def logout = Action { req =>
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  def index = MaybeAuthenticated { mpo =>  r =>

    
    Ok(html.index(mpo))
  }

  def showHelloForm = IsAuthenticated { dhSession => implicit request =>
    Ok(html.index(dhSession))
  }

  def fbauth = Action { implicit req =>
    import facebookLoginManager._
    val res = 
      for(acCode <- obtainAuthorizationCode(req.queryString).left;
          authToken <- obtainAccessToken(acCode).left;
          info <- obtainMinimalInfo(authToken.value).left) 
      yield info

    res match {
      case Left(info) => transaction {
        val facebookId = java.lang.Long.parseLong(info.id)
        val (u, isNewUser) = 
        Schema.users.where(_.facebookId === facebookId).headOption match {
          case None =>
            Logger.info("New User registered, facebookId :  " + facebookId)
            (createNewUser(info, facebookId), true)
          case Some(uz) => 
            Logger.info("Successful Logon, userId: " + uz.id)
            (uz, false)
        }

        val ses = new DecisionHubSession(u, req)

        AuthenticationSuccess(Redirect(routes.Application.index), ses)
      }
      case Right(error) => 
        Logger.info("Failed Logon " + res)
        Redirect(routes.Application.login)
    }
  }
  
  private def createNewUser(info: MinimalInfo, fbId: Long) = {

    val u = User(info.firstName, info.lastName, info.name, Some(fbId), info.email, None)
    
    Schema.users.insert(u)    
  }
}



object OAuthErrorTypes extends Enumeration {
  type OAuthErrorTypes = Value 
  
  val PermissionNotGranted, UnexpectedResponse = Value
}

case class AuthorizationToken(value: String, numberOfSecondsUntilExpiry: Int)

case class MinimalInfo(id: String, firstName: Option[String], lastName: Option[String], name: Option[String], email: Option[String])

class FacebookOAuthManager(val appKey: String, appSecret: String, loginRedirectFromFacebook: String, extraArgs: Map[String,String] = Map.empty) {

//https://graph.facebook.com/APP_ID/accounts/test-users?installed=true&name=zaza1&locale=en_US&permissions=read_stream&method=post&access_token=APP_ACCESS_TOKEN

  
  import OAuthErrorTypes._

  def loginWithFacebookUrl =
    "https://www.facebook.com/dialog/oauth?" +
      "&client_id=" + appKey +
      "&redirect_uri=" + loginRedirectFromFacebook


  def obtainAccessToken(code: String) = {

    val u =
      WS.url("https://graph.facebook.com/oauth/access_token").withQueryString(
        "client_id" -> appKey,
        "redirect_uri" -> loginRedirectFromFacebook, 
        "client_secret" -> appSecret,
        "code" -> code
      ).get

    val res = 
      u.map { response =>
        val txt = response.body
        txt.split('&').flatMap(_.split('=')).toList match {
          case List("access_token", accessToken,"expires", secondsUntilExpiry) => 
            Left(AuthorizationToken(accessToken, Integer.parseInt(secondsUntilExpiry)))
          case _  => 
            logUnexpectedResponse("getting access token", txt)
            Right(UnexpectedResponse)
          }
      }.await.get
    res
  }

  def logUnexpectedResponse(requestName: String, response: String) = 
    println("Unexpected response while " + (requestName, response))

  def obtainAuthorizationCode(args: Map[String,Seq[String]]) = {

    def error = {
      logUnexpectedResponse("getting authorization code", args.mkString)
      Right(UnexpectedResponse)
    }

    args.get("code") match {
      case Some(Seq(accessCode)) => 
        Left(accessCode)
      case None => args.get("error_reason") match {
        case Some(Seq("user_denied")) => Right(PermissionNotGranted)
        case _ => error
      }
      case _ => error
    }
  }

  def obtainMinimalInfo(accessToken: String): Either[MinimalInfo,OAuthErrorTypes.Value] = 
    try {

      val meUrl = 
        WS.url("https://graph.facebook.com/me").withQueryString( 
          "access_token" -> accessToken
        ).get

      meUrl.map { resp => 
        val js = resp.json

        val r = MinimalInfo(
         (js \ "id").as[String],
         (js \ "first_name").as[Option[String]],
         (js \ "last_name").as[Option[String]],
         (js \ "name").as[Option[String]],
         (js \ "email").as[Option[String]]
        )

        Left(r)
      }.await.get
    }
    catch {
      case e:Exception =>
        Logger.error("unexpected response from facebook graph api " + e.toString)
        Right(UnexpectedResponse)
    }
}
