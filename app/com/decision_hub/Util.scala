package com.decision_hub

import play.api.mvc.Results._
import play.api.Logger
import play.api.mvc._
import com.codahale.jerkson.{Json => Jerkson}
import com.decision_hub.FacebookProtocol.FBClickOnApplication
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import models.User

object Util {

  def now = System.currentTimeMillis
  
  class JavascriptEscaper(s: String) {
    def encodedAsJavascript = 
      encodeAsJavascript(s)
  }
  
  def asJavascriptNullable(s: Option[String]) =
    s.map(x => "'" + x + "'").getOrElse("null")
    
  def asJavascriptObject(s: Option[String]) =
    s.getOrElse("null")
  
  def encodeAsJavascript(s: String) = s.replace("\'","\\'").replace("\"","\\\"")
  
  implicit def string2JavascriptEscaper(s: String) = new JavascriptEscaper(s)
  
  private def allHeaders(headers: RequestHeader) = 
    for(h <- headers.headers.toMap)
      yield (h._1, h._2.mkString("[",",","]"))

  
  def dump(headers: RequestHeader) =
    Seq(
      //"isAuthenticated: " + isAuthenticatedz,
      "Method:" +  headers.method,
      "URL: " + headers.path + headers.rawQueryString,
      "Headers: \n" + allHeaders(headers).mkString("\n")
    ).mkString("\n")
    


  def encodeLong(v: Long) = {
    // Every long has 64 bits, and we need 11 segments of 6 bits to represent it.
    // The characters used are letters, digits, underscore and dash.
    val uuidChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
    (0 until 11).map(i => uuidChars(((v >> (i * 6)) & 63).toInt)).mkString
  }

  def newGuid = {
    import java.util.UUID
    val uuid = UUID.randomUUID
    encodeLong(uuid.getMostSignificantBits) + encodeLong(uuid.getLeastSignificantBits)
  }
  
  def parseLong(s: String) =
    java.lang.Long.parseLong(s)

  def parseInt(s: String) =
    java.lang.Integer.parseInt(s)    
    
  def parseLongOption(s: String) =
    try {Some(java.lang.Long.parseLong(s))}
    catch {case e:NumberFormatException => None}
    
    
  private def logger = Logger("application")
  
  def expect[A](implicit bodyParser: CustomBodyParser[A]) = bodyParser.parser
  

  def expectJson[A](implicit m: Manifest[A]) = 
    BodyParsers.parse.tolerantText.map { jsonText => 
      try {
        Jerkson.parse(jsonText)(m)
      }
      catch {
        case e:Exception =>
          logger.error("bad json request, cannot create a '" + 
             m.erasure.getCanonicalName + "' from :\n" + jsonText, e)
          throw e
      }
    }

  def expectRawJson[A <: {def validate:Either[B => B,Map[String,String]]},B](implicit m: Manifest[A]) = 
    BodyParsers.parse.tolerantText.flatMap { jsonText =>

      val e = 
        try {
          Jerkson.parse(jsonText)(m).validate match {
            case Left(b) =>  Right(b)
            case Right(m) => Left(BadRequest(Jerkson.generate(m)) : Result)
          }
        }
        catch {
          case e:Exception =>
            logger.error("bad json request, cannot create a '" + m.erasure.getCanonicalName + "' from :\n" + jsonText, e)
            Left(BadRequest("null") : Result)
        }

      BodyParser(rh => Done(e,Input.Empty))
    }

  implicit object booleanLongMapBodyParser extends CustomBodyParser[Map[Long,Boolean]] {
    def parser = BodyParsers.parse.urlFormEncoded.map { m =>
      
      println("---->" + m)
      
      m.map(_ match {
        case (k,Seq("true"))  => (parseLong(k), true)
        case (k,Seq("false")) => (parseLong(k), false)
      })
    }
  }

  implicit object longIntMapBodyParser extends CustomBodyParser[Map[Long,Int]] {
    def parser = BodyParsers.parse.urlFormEncoded.map( m =>
      m.map(_ match {
        case (k,Seq(i)) => (parseLong(k), parseInt(i))
      })
    )
  }

  implicit object booleanStringMapBodyParser extends CustomBodyParser[Map[String,Boolean]] {
    def parser = BodyParsers.parse.urlFormEncoded.map { m =>
      
      println("---->" + m)
      
      m.map(_ match {
        case (k,Seq("true"))  => (k, true)
        case (k,Seq("false")) => (k, false)
      })
    }
  }

  implicit object stringIntMapBodyParser extends CustomBodyParser[Map[String,Int]] {
    def parser = BodyParsers.parse.urlFormEncoded.map( m =>
      m.map(_ match {
        case (k,Seq(i)) => (k, parseInt(i))
      })
    )
  }
  
  implicit object fbClickOnApplicationBodyParser extends CustomBodyParser[FBClickOnApplication] {
    def parser = BodyParsers.parse.urlFormEncoded.map { m =>
      FacebookProtocol.authenticateSignedRequest(m).
        getOrElse(sys.error("invalid signed request " + m))
    }
  }
  
  implicit def fbInfo2User(i: MinimalInfo) = {
    val u = 
      User(facebookId = Some(java.lang.Long.parseLong(i.id)), 
         firstName = i.firstName,
         lastName = i.lastName,
         nickName = i.firstName,
         email = i.email)
    assert(u.validate.isLeft, "invalid user " + i)
    u
  }
}

trait CustomBodyParser[A] {
  def parser: BodyParser[A]
}

  
abstract class ValidationBlock[A](implicit logger: Logger) {
  
  implicit def string2Long(s: String) = 
    java.lang.Long.parseLong(s)

  implicit def string2Int(s: String) = 
    java.lang.Integer.parseInt(s)

  def keepLeft(s: String, c: Char) = 
    s.split(c)(0)
    
  def keepRight(s: String, c: Char) = 
    s.split(c)(1)

  protected def value: A
 
  private def extract = {    
    try {
      Left(value)
    }
    catch {
      case e:Exception => Right(e)
    }
  }

  protected def get[B](b: =>B, errorMsg: String) =
    try {
      b
    }
    catch {
      case e:Exception =>
        logger.error(errorMsg)
        throw e
    }

  def extractValid(f: A => Result): Result =
    extract.fold(
      a => f(a),
      ex => {
        logger.error("Invalid input", ex)
        BadRequest
      }
    )
}