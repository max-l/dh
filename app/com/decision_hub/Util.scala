package com.decision_hub


object Util {

  def parseLong(s: String) =
    java.lang.Long.parseLong(s)

    
  def parseLongOption(s: String) =
    try {Some(java.lang.Long.parseLong(s))}
    catch {case e:NumberFormatException => None}
    
    
  
  
  
}

trait ValidationBlock[A] {
  
  implicit def string2Long(s: String) = 
    java.lang.Long.parseLong(s)

  implicit def string2Int(s: String) = 
    java.lang.Integer.parseInt(s)

  def keepLeft(s: String, c: Char) = 
    s.split(c)(0)
    
  def keepRight(s: String, c: Char) = 
    s.split(c)(1)
    
  protected def value: A 

  def extract = 
    try {
      Left(value)
    }
    catch {
      case e:Exception => Right(e)
    }
}