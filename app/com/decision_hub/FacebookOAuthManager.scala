package com.decision_hub
import play.api.libs.ws.WS
import play.api.Logger
import play.api.libs.json.Json
import com.strong_links.crypto._
import play.api.Play
import play.api.mvc.Call
import play.api.templates.Html
import play.api.libs.json.JsValue
import java.net.URLEncoder
import play.api.libs.concurrent.Promise
import javax.xml.ws.Response

object OAuthErrorTypes extends Enumeration {
  type OAuthErrorTypes = Value 
  
  val PermissionNotGranted, UnexpectedResponse = Value
}

case class AuthorizationToken(value: String, expiryTime: Long) {
  def this(v: String, exp: String) = this(v, Integer.parseInt(exp) * 1000 + System.currentTimeMillis)
}

case class MinimalInfo(id: String, firstName: Option[String] = None, lastName: Option[String] = None, name: Option[String] = None, email: Option[String] = None)

class FacebookOAuthManager(val appKey: String, appSecret: String, loginRedirectFromFacebook: String, extraArgs: Map[String,String] = Map.empty) extends CryptoUtil {

//https://graph.facebook.com/APP_ID/accounts/test-users?installed=true&name=zaza1&locale=en_US&permissions=read_stream&method=post&access_token=APP_ACCESS_TOKEN

  val appSecretField = appSecret : CryptoField
  
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
      )

    println("acc tok url : \n" + u.url + "/" + u.queryString)
      
    val res = 
      u.get.map { response =>
        val txt = response.body
        txt.split('&').flatMap(_.split('=')).toList match {
          case List("access_token", accessToken,"expires", secondsUntilExpiry) => 
            Left(new AuthorizationToken(accessToken, secondsUntilExpiry))
          case _  => 
            logUnexpectedResponse("getting access token", txt)
            Right(UnexpectedResponse)
          }
      }.await(15 * 1000).get
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
  
  def logger = Logger("application")
  
  private def _parseLong(s: String) = 
    try {
      java.lang.Long.parseLong(s)
    }
    catch {
      case nfe: NumberFormatException => {
        logger.error(nfe.toString())
        throw nfe
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


object FacebookProtocol extends CryptoUtil {
  
  def logger = Logger("application")
  
  private def mandatoryStringConfig(n: String) =
    Play.current.configuration.getString(n).getOrElse("missing config '"+n + "'")
  
  def facebookAppId = 
    mandatoryStringConfig("application.FacebookAppId")

  private def facebookSecret = 
    mandatoryStringConfig("application.FacebookSecret")
    
  private def applicationDomainName = 
    mandatoryStringConfig("application.domainName")
  
  val facebookOAuthManager = new FacebookOAuthManager(
    facebookAppId, facebookSecret, "https://"+applicationDomainName)

  val loginRedirectUrl = facebookOAuthManager.loginWithFacebookUrl
  
  sealed trait FBClickOnApplication
  sealed case class FBClickOnApplicationNonRegistered(jsonMsg: JsValue) extends FBClickOnApplication
  sealed case class FBClickOnApplicationRegistered(fbUserId: Long) extends FBClickOnApplication

  private def padBas64(s: String) = 
    (s.length() % 4) match {
      case 0 => s
      case 1 => s + "==="
      case 2 => s + "=="
      case 3 => s + "="
    }
  
  def authenticateSignedRequest(signedRequest: String): Option[JsValue] = 
    signedRequest.split('.').toList match {
      case List(_sig, __req) =>
          val paddedReq = padBas64(__req)

          val reqBin = Base64.decode(paddedReq, Base64.URL_SAFE)
          val reqStr = new String(reqBin)
          val js = Json.parse(reqStr)

          val algo = (js \ "algorithm").as[Option[String]]
          val isValid = algo match {
            case None => false
            case Some("HMAC-SHA256") => 
              val sig : CryptoField = _sig
              val req : CryptoField = __req
              
              val computedSig = hmacSha256(req)(facebookOAuthManager.appSecretField)
              val z1 = sig.value + "="
              val z2 = computedSig.toUrlSafe

              val res = z1 == z2
              if(!res)
                logger.warn("Invalid authentication from Facebook.")
              res
          }

          logger.info("Authentication from Facebook status :" + isValid)
          if(isValid) Some(js) else None
      case _ => 
        logger.error("bad facebook signed_request format :'%s'".format(signedRequest))
        None
    }

  def authenticateSignedRequest(r: Map[String,Seq[String]]): Option[FBClickOnApplication] = {

    r.get("signed_request").flatten.toSeq match {
      case Seq(sreq) =>
        authenticateSignedRequest(sreq).flatMap { validReqJson =>
          (validReqJson \ "user_id").asOpt[String] match {
            case None => Some(FBClickOnApplicationNonRegistered(validReqJson))
            case Some(sUserId) => parseLong(sUserId) match {
              case None =>
                logger.error("invalid facebook userId ")
                None
              case Some(uId) => Some(FBClickOnApplicationRegistered(uId)) 
            }
          }
        }
      case _ => None
    }
/*    
    val jsonSignedRequest = 
      for(signedReqSeq <- r.get("signed_request").flatten;
          req <- authenticateSignedRequest(signedReqSeq)
      yield req
        
      jsonSignedRequest match {
        case None =>
          logger.error("bad signed_request format :'" + r + "'")
          None
        case Some((_sig, __req)) =>

          val isValid = authenticateSignedRequest(__req)

          logger.info("Authentication from Facebook status :" + isValid)
          if(! isValid)
            None
          else
            (js \ "user_id").asOpt[String] match {
              case None => Some(FBClickOnApplicationNonRegistered(reqStr))
              case Some(sUserId) => parseLong(sUserId) match {
                case None =>
                  logger.error("invalid facebook userId ")
                  None
                case Some(uId) => Some(FBClickOnApplicationRegistered(uId)) 
              }
            }
          
      }
*/      
  }
  
  case class FBAppOAuthToken(tok: String, exipryTime: Long)
  
  
  private var cachedAppToken: Option[AuthorizationToken] = None
  
  private def appToken(clientId: String, clientSecret: String) = cachedAppToken.getOrElse {

    WS.url("https://graph.facebook.com/oauth/access_token").withQueryString(
      "grant_type"->"client_credentials",
      "grant_type" -> "client_credentials",
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ).get.map { r =>
       val t = AuthorizationToken(URLEncoder.encode(r.body.split('=')(1),"UTF-8"), Long.MaxValue)
       cachedAppToken = Some(t)
       t
    }.await(1000 * 30).get
  }
  
  def appAccessToken = appToken(facebookAppId, facebookSecret)
  
  def lookupAppRequestInfo(requestId: Long) = {

    WS.url("https://graph.facebook.com/" + requestId).
     withQueryString("access_token" -> appAccessToken.value).get.map { r =>
        com.codahale.jerkson.Json.parse[FBAppRequestInfo](r.body)
    }
  }
  
  def lookupAppRequestInfoRaw(requestId: Long) = {

    WS.url("https://graph.facebook.com/" + requestId).
     withQueryString("access_token" -> appAccessToken.value).get.map { r =>
        (r.body, com.codahale.jerkson.Json.parse[FBAppRequestInfo](r.body))
    }
  }  
  
  def deleteAppRequest(requestId: Long, fbUserId: Long) =
    zdeleteAppRequest(requestId, fbUserId, appAccessToken.value)

  
  
  def zdeleteAppRequest(requestId: Long, fbUserId: Long, appToken: String) = {
    val u = WS.url("https://graph.facebook.com/" + requestId + "_" + fbUserId + "/").
       withQueryString("access_token" -> appToken)
    
    logger.debug("FB api call : \n" + u.url + u.queryString)
    logger.debug(u.headers.mkString("\n"))
    u.delete
  }  
}

case class FBAppRequestInfoFrom(id: Long, name: String)
case class FBAppRequestInfo(id: String, from: FBAppRequestInfoFrom, created_time: String, data: String) {
  
  def senderIconUri =
    "https://graph.facebook.com/"+from.id+"/picture"
}

  
