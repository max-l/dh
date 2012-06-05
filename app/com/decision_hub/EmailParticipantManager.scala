package com.decision_hub

import models.Schema._
import models._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import play.api.Logger
import models.DecisionParticipation


object EmailParticipantManager {

  def logger = Logger("application")

  def lookupUserByEmail(email: String) = inTransaction {
    users.where(_.email === Some(email)).headOption
  }
}