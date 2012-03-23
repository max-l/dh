package com.decision_hub


object Util {

  def parseLong(s: String) =
    java.lang.Long.parseLong(s)

    
  def parseLongOption(s: String) =
    try {Some(java.lang.Long.parseLong(s))}
    catch {case e:NumberFormatException => None}
    
}