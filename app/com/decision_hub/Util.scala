package com.decision_hub

import play.api.mvc.Results._
import play.api.Logger
import play.api.mvc._
import com.codahale.jerkson.{Json => Jerkson}
import com.decision_hub.FacebookProtocol.FBClickOnApplication
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise

object Util {

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
  
  //val zz = new Iteratee[String,Int] 
  val zz = 
    BodyParsers.parse.tolerantText

  //BodyParsers.parse.tolerantText(rh).pureFold()

  val consumeOneInputAndEventuallyReturnIt = new Iteratee[String,Int] {
  
    def fold[B](
      done: (Int, Input[String]) => Promise[B],
      cont: (Input[String] => Iteratee[String, Int]) => Promise[B],
      error: (String, Input[String]) => Promise[B]
    ): Promise[B] = {
      cont(in => Done(1, Input.Empty))
    }
  }

  Cont[String,Int](_  match { 
    case Input.El(e) => Done[String,Int](1, Input.Empty)
    case _ => Error[String]("!!!", Input.Empty)
  })

  def qq[A]: (RequestHeader) => Iteratee[Array[Byte], Either[Result, A]] = null
 
  def s2a[A](s: String): A = sys.error("!")
  def rh: RequestHeader = sys.error("!")
  /*
  val bp1 = new BodyParser[Int] {
    def apply(h: RequestHeader) = {
      val i = BodyParsers.parse.tolerantText(h)
    }
  }
*/  
/*  
  def bp[A] = new BodyParser[A] {
    def apply { (rh: RequestHeader) =>
      BodyParsers.parse.tolerantText(rh).fold(
        done => done._1,
        
      )
    }
  }
*/  
/*  
  def expectRawJson[A <: {def validate:Either[B,Map[String,String]]},B](implicit m: Manifest[A]) = 
    BodyParsers.parse.tolerantText.andThen { jsonText =>
      
      jsonText
      
      val a = 
        try {
          Jerkson.parse(jsonText)(m)
        }
        catch {
          case e:Exception =>
            logger.error("bad json request, cannot create a '" + 
               m.erasure.getCanonicalName + "' from :\n" + jsonText, e)
            throw e
        }
      a.validate match {
        Left(b) => b
      }
    }
*/
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
      FacebookProtocol.authenticateSignedRequest(m).
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