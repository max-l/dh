package com.decision_hub

import models._
import controllers.DecisionHubSession
import org.squeryl.PrimitiveTypeMode._ 
import play.Logger

object AuthenticationManager {

  
  def createNewUser(info: MinimalInfo, fbId: Long) = {

    val u = User(info.firstName, info.lastName, info.name, Some(fbId), true, info.email)
    
    Schema.users.insert(u)    
  }

  //def authenticateUserFrom
  
  def lookupFacebookUser(fbId: Long) = inTransaction {
    Schema.users.where(_.facebookId === fbId).headOption
  }
  
  def lookupUser(userId: Long) = inTransaction {
    Schema.users.lookup(userId).headOption
  }  

  def authenticateOrCreateUser(info: MinimalInfo) = {
    val facebookId = java.lang.Long.parseLong(info.id)
    val (u, isNewUser) = 
    Schema.users.where(_.facebookId === facebookId).headOption match {
      case None =>
        Logger.info("New User registered, facebookId :  " + facebookId)
        (AuthenticationManager.createNewUser(info, facebookId), true)
      case Some(uz) => 
        Logger.info("Successful Logon, userId: " + uz.id)
        (uz, false)
    }
    
    u
  }
}