package com.decision_hub

import models.Schema._
import models._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp


object DecisionManager {


  def acceptInvitation(requestIds: Iterable[Long]) = transaction {
    update(decisionParticipations)(dp =>
      where(dp.requestId in requestIds)
      set(
        dp.accepted := true,
        dp.timeAcceptedOrRefused := Some(new Timestamp(System.currentTimeMillis))
      )
    )
  }

  def createParticipations(requestId: Long, decisionId: Long, reciptientFacebookIds: Seq[Long]) = transaction {

    val voterIds =
      from(Schema.users)(u =>
        where(u.facebookId in reciptientFacebookIds)
        select(&(u.id))
      )

    val participations = 
      for(inviteIdstr <- voterIds) 
        yield DecisionParticipation(
          decisionId,
          requestId,
          inviteIdstr, 
          new Timestamp(System.currentTimeMillis), 
          false, 
          None)


    Schema.decisionParticipations.insert(participations.toSeq)
  }
}