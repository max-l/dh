package com.decision_hub

import play.api.mvc.Results._
import play.api.Logger
import play.api.mvc.Result

object Util {

  def parseLong(s: String) =
    java.lang.Long.parseLong(s)

    
  def parseLongOption(s: String) =
    try {Some(java.lang.Long.parseLong(s))}
    catch {case e:NumberFormatException => None}
    
    
  
  
  
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