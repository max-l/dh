package com.decision_hub

import com.strong_links.crypto._
import models.Schema._
import models._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import org.squeryl.dsl.ast.LogicalBoolean
import play.api.Logger

object PersistentLoginManager {

  import Util._
  
  def logger = Logger("application")
  
  def persistentLoginMaxDurationInMinutes = 60 * 24 * 30

  def nextPersistentLoginExpiry = 
    now + (persistentLoginMaxDurationInMinutes * 60 * 1000)


  def createInternalLogin(email: String, password: String) = {

    val h1 = BCrypt.hashpw(password, BCrypt.gensalt(12));
  }

  def validateInternalLogin(userId: Long, password: String) =
    for(u <- Schema.users.where(_.id === userId).headOption;
        uEmail <- u.email;
        uPasswordHash <- u.passwordHash if BCrypt.checkpw(password, uPasswordHash)) 
     yield u

  private def cleanupExpiredPersistentLogins(userId: String) =
    Schema.persistentLogins.deleteWhere(pl => pl.expiryTime lt now and pl.userId === userId)

  def activateOrRenewPersistentLogin(userId: String) = transaction { // we preffer to commit asap

    cleanupExpiredPersistentLogins(userId)

    Schema.persistentLogins.insert(new PersistentLogin(userId, nextPersistentLoginExpiry))
  }

  def validatePersistentLogin(toValidate: PersistentLogin) = transaction { // we preffer to commit asap

    import toValidate._
    import Schema._

    cleanupExpiredPersistentLogins(userId)

    val res =
      persistentLogins.where(pl =>
        pl.userId === userId and
        pl.serieId === serieId
      ).toList

    res match {
      case Nil => None
      case List(pl) =>
        if(pl.token != toValidate.token) {
          persistentLogins.deleteWhere(_.userId === userId)
          logger.warn("Cookie theft occured.")
          None
        }
        else {

          val renewed = renew(nextPersistentLoginExpiry)

          update(persistentLogins)(pl =>
            where(pl.userId === userId and pl.serieId === serieId)
            set(pl.serieId := renewed.serieId,
                pl.expiryTime := renewed.expiryTime)
          )

          Some(renewed)
        }
      case _ => sys.error("More than one persistent login cookie found for " + (userId, serieId))
    }
  }
}