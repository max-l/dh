package com.decision_hub
import play.api.libs.ws.WS
import play.api.Logger
import play.api.libs.json.Json
import com.strong_links.crypto._

object OAuthErrorTypes extends Enumeration {
  type OAuthErrorTypes = Value 
  
  val PermissionNotGranted, UnexpectedResponse = Value
}

case class AuthorizationToken(value: String, numberOfSecondsUntilExpiry: Int)

case class MinimalInfo(id: String, firstName: Option[String], lastName: Option[String], name: Option[String], email: Option[String])

class FacebookOAuthManager(val appKey: String, appSecret: String, loginRedirectFromFacebook: String, extraArgs: Map[String,String] = Map.empty) extends CryptoUtil {

//https://graph.facebook.com/APP_ID/accounts/test-users?installed=true&name=zaza1&locale=en_US&permissions=read_stream&method=post&access_token=APP_ACCESS_TOKEN

  val appSecretField = appSecret : CryptoField
  
  import OAuthErrorTypes._

  def loginWithFacebookUrl =
    "https://www.facebook.com/dialog/oauth?" +
      "&client_id=" + appKey +
      "&redirect_uri=" + loginRedirectFromFacebook


  def loginWithFacebookUrl(path: String) =
    "https://www.facebook.com/dialog/oauth?" +
      "&client_id=" + appKey +
      "&redirect_uri=" + path
      
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

  sealed trait FBClickOnApplication
  sealed case class FBClickOnApplicationNonRegistered(jsonMsg: String) extends FBClickOnApplication
  sealed case class FBClickOnApplicationRegistered(fbUserId: Long) extends FBClickOnApplication

  def authenticateSignedRequest(r: Map[String,Seq[String]])(implicit m: FacebookOAuthManager): Option[FBClickOnApplication] = {

    val jsonSignedRequest = 
      for(signedReqSeq <- r.get("signed_request");
          signedReq  <- signedReqSeq.map(_.split('.').toList).headOption;
          sr <- signedReq match {
            case List(sig, req) => Some((sig, req))
            case _ => None
          })
      yield sr
        
      jsonSignedRequest match {
        case None =>
          logger.error("bad signed_request format :'" + r + "'")
          None
        case Some((_sig, __req)) =>

          if(__req.length() % 4 > 0)
            sys.error("Bad Facebook signed request, Base64 should have a length multiple of 4 :" + __req)

          val reqBin = Base64.decode(__req, Base64.URL_SAFE)
          val reqStr = new String(reqBin)
          val js = Json.parse(reqStr)

          val algo = (js \ "algorithm").as[Option[String]]
          val isValid = algo match {
            case None => false
            case Some("HMAC-SHA256") => 
              val sig : CryptoField = _sig
              val req : CryptoField = __req
              
              val computedSig = hmacSha256(req)(m.appSecretField)
              val z1 = sig.value + "="
              val z2 = computedSig.toUrlSafe

              val res = z1 == z2
              if(!res)
                logger.warn("Invalid authentication from Facebook.")
              res
          }

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
  }
  
}