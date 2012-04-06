package com.decision_hub

import play.api.mvc.Results._
import play.api.Logger
import play.api.mvc._
import com.codahale.jerkson.{Json => Jerkson}
import com.decision_hub.FacebookProtocol.FBClickOnApplication

object Util {

  def parseLong(s: String) =
    java.lang.Long.parseLong(s)

  def parseInt(s: String) =
    java.lang.Integer.parseInt(s)    
    
  def parseLongOption(s: String) =
    try {Some(java.lang.Long.parseLong(s))}
    catch {case e:NumberFormatException => None}
    
    
  
  def expect[A](implicit bodyParser: CustomBodyParser[A]) = bodyParser.parser
  

  def expectJson[A](implicit m: Manifest[A]) =
    BodyParsers.parse.tolerantText.map (Jerkson.parse(_)(m))

  implicit object booleanLongMapBodyParser extends CustomBodyParser[Map[Long,Boolean]] {
    def parser = BodyParsers.parse.urlFormEncoded.map( m =>
      m.map(_ match {
        case (k,Seq("true"))  => (parseLong(k), true)
        case (k,Seq("false")) => (parseLong(k), false)
      })
    )
  }

  implicit object longIntMapBodyParser extends CustomBodyParser[Map[Long,Int]] {
    def parser = BodyParsers.parse.urlFormEncoded.map( m =>
      m.map(_ match {
        case (k,Seq(i)) => (parseLong(k), parseInt(i))
      })
    )
  }

  implicit object fbClickOnApplicationBodyParser extends CustomBodyParser[FBClickOnApplication] {
    def parser = BodyParsers.parse.urlFormEncoded.map { m =>
      FacebookProtocol.authenticateSignedRequest(m)(controllers.facebookOAuthManager).
        getOrElse(sys.error("invalid signed request " + m))
    }
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