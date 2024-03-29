package com.decision_hub

import models.Schema._
import models._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import org.squeryl.dsl.ast.LogicalBoolean
import play.api.Logger
import models.DecisionParticipation
import controllers.AccessKey





object FacebookParticipantManager {

  def logger = Logger("application")
  
  def usersByFbId(fbIds: Traversable[Long]) =
    from(Schema.users)(u =>
      where { 
        val fbId = u.facebookId.~

        val r = fbId.in(fbIds)

        r
      }
      select(&(u.id), &(u.facebookId))
    )
    
  def lookupFacebookUser(fbId: Long) = inTransaction {
    Schema.users.where(_.facebookId === fbId).headOption
  }
  
  def lookupDecisionIdForAccessGuid(publicAccessGuid: String) = inTransaction {
    
    from(Schema.pTokens, Schema.decisions)((t,d) =>
      where(t.decisionId === d.id and t.id === publicAccessGuid)
      select(d)
    ).single
  }
  
  def inviteVotersFromFacebook(k: AccessKey, r: FBInvitationRequest) = k.attemptAdmin { (currentUserId: Long) => inTransaction {

    val recipientsFacebookIds = r.to.map(_.uid).toSet

    val usersAlreadyInSystem = 
      usersByFbId(recipientsFacebookIds).toMap

    val fbUserIdsToInsert =
      recipientsFacebookIds.diff(usersAlreadyInSystem.map(_._2.get).toSet)

    val usersToInsert = 
      for(fbInfo <- r.to if fbUserIdsToInsert.contains(fbInfo.uid))
        yield User(nickName = Some(fbInfo.name), facebookId = Some(fbInfo.uid), confirmed = false)

    
    logger.debug("Will insert new FB users : " + usersToInsert)
    
    users.insert(usersToInsert)

    val alreadyParticipantUserIds = 
      from(decisionParticipations)(dp =>
        where(dp.decisionId === k.decision.id and dp.voterId.in(usersAlreadyInSystem.map(_._1).toSeq))
        select(&(dp.voterId))
      ).toSet
    
    logger.debug("alreadyParticipantUserIds : " + alreadyParticipantUserIds)
    
    val z = 
      if(fbUserIdsToInsert.isEmpty)
        Map.empty
      else
        usersByFbId(fbUserIdsToInsert).toMap
/*    
    val invitationsToInsert =
      for(u <- (z ++ usersAlreadyInSystem) if ! alreadyParticipantUserIds.contains(u._1))
        yield ParticipationInvitation(
          decisionId = k.decision.id,
          facebookAppRequestId = r.request,
          invitedUserId = u._1,
          invitingUserId = currentUserId)

    logger.debug("invitationsToInsert : " + invitationsToInsert)

    participationInvitations.insert(invitationsToInsert)
*/
    // automatic acceptation : 
    val dps = 
      for(u <- (z ++ usersAlreadyInSystem) if ! alreadyParticipantUserIds.contains(u._1))
        yield DecisionParticipation(k.decision.id, u._1, true, Some(r.request))
    
    decisionParticipations.insert(dps)

/*    
    from(participationInvitations, users)((pi, u) =>
      where(pi.decisionId === r.decisionId and pi.invitedUserId === u.id)
      select(&(u.f))
    )
    //exclude_ids

*/
  }}
  
  def authenticateOrCreateUser(info: MinimalInfo) = {
    val facebookId = java.lang.Long.parseLong(info.id)
    val (u, isNewUser) = 
    Schema.users.where(_.facebookId === facebookId).headOption match {
      case None =>
        Logger.info("New User registered, facebookId :  " + facebookId)
        (createNewUser(info, facebookId), true)
      case Some(uz) => 
        Logger.info("Successful Logon, userId: " + uz.id)
        (uz, false)
    }
    
    u
  }
  
  def createNewUser(info: MinimalInfo, fbId: Long) = {

    val u = User(info.firstName, info.lastName, info.name, Some(fbId), true, info.email)
    
    Schema.users.insert(u)    
  }  

  def respondToAppRequestClick(facebookRequestId: Option[Long], targetFacebookUserId: Long) = inTransaction {
 
    facebookRequestId.foreach { fbReqId =>
      val dId = 
        from(decisionParticipations)(dp => 
          where(dp.facebookRequestId === fbReqId)
          select(dp.decisionId)
        ).headOption
      
      if(dId == None) 
        logger.error("AppRequest "+ fbReqId + " Not found, TODO: DELETE app Requests, and handle AppRequest Not Found")
      else
        update(decisionParticipations)(dp => 
          where(dp.decisionId === dId.get)
          set(dp.lastModifTime := new Timestamp(System.currentTimeMillis))
        )
    }
    
    val u = 
      from(users)(u => 
        where(u.facebookId === targetFacebookUserId)
        select(u)
      ).headOption

    u
  }
  
}