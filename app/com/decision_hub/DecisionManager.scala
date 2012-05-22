package com.decision_hub

import models.Schema._
import models._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import org.squeryl.dsl.ast.LogicalBoolean
import play.api.Logger
import models.DecisionParticipation


object DecisionManager {

  def logger = Logger("application")
  
  def newDecision = inTransaction {
    decisions.insert(Decision(0L, ""))
  }
  
  def decisionExists(dId: String) = inTransaction {
    decisions.lookup(dId).isDefined
  }
  
  def updateDecision(d: Decision) = inTransaction {
    decisions.update(d)
    true
  } 
  
  def getDecision(decisionId: String) = inTransaction {  
    decisions.lookup(decisionId)
  }
  
  def getAlternatives(decisionId: String) = inTransaction {
    decisionAlternatives.where(a => a.decisionId === decisionId).toList
  }
  
  def getBallot(decisionId: String, voterId: Long): Ballot = inTransaction {

    val d = decisions.lookup(decisionId).get
    
    getBallot(d, voterId)
  }

  private def getBallot(decision: Decision, voterId: Long): Ballot = {
    
    //if(!isParticipant(decisionId, voterId))
      //sys.error(voterId + " not participant in " + decisionId)

    val alts = decisionAlternatives.where(a => a.decisionId === decision.id).toList

    val scores =
      votes.where(v => v.decisionId === decision.id and v.voterId === voterId).toList

    val resAlts =
      for(a <- alts)
        yield scores.find(_.alternativeId == a.id) match {
          case None    => Score(a.id, a.title, None)
          case Some(s) => Score(a.id, a.title, Some(s.score))
        }

    new Ballot(decision.id, decision.title, resAlts)
  }
  
  def vote(decisionId: String, alternativeId: Long, voterId: Long, score: Int) = inTransaction {
    
    //TODO: verify if participant
    
    val v = 
      update(votes)(v =>
        where(v.decisionId === decisionId and v.alternativeId === alternativeId and v.voterId === voterId)
        set(v.score := score)
      )
      
    if(v < 1) 
      votes.insert(new Vote(decisionId, alternativeId, voterId, score))
  }

  def createAlternative(decisionId: String, title: String) = inTransaction {
    
    decisionAlternatives.insert(DecisionAlternative(decisionId, title))
  }
  
  def updateAlternative(decisionId: String, alternativeId: Long, title: String) = inTransaction {

    update(decisionAlternatives)(a =>
      where(a.id === alternativeId)
      set(a.title := title)
    )
  }
  
  def deleteAlternative(decisionId: String, alternativeId: Long) = inTransaction {

    decisionAlternatives.deleteWhere(a => a.id === alternativeId)
  }  
  
  def inviteVotersFromFacebook0(invitingUserId: Long, r: FBInvitationRequest) = inTransaction {

    val recipientsFacebookIds = r.to.map(_.uid).toSet

    val usersAlreadyInSystem = 
      usersByFbId(recipientsFacebookIds).toMap

    val fbUserIdsToInsert =
      recipientsFacebookIds.diff(usersAlreadyInSystem.map(_._2.get).toSet)

    val usersToInsert = 
      for(fbInfo <- r.to if fbUserIdsToInsert.contains(fbInfo.uid))
        yield User(nickName = Some(fbInfo.name), facebookId = Some(fbInfo.uid))

    logger.debug("Will insert new FB users : " + usersToInsert)
    
    users.insert(usersToInsert)

    val alreadyParticipantUserIds = 
      from(decisionParticipations)(dp =>
        where(dp.decisionId === r.decisionId and dp.voterId.in(usersAlreadyInSystem.map(_._1).toSeq))
        select(&(dp.voterId))
      ).toSet
    
    logger.debug("alreadyParticipantUserIds : " + alreadyParticipantUserIds)
    
    val z = 
      if(fbUserIdsToInsert.isEmpty)
        Map.empty
      else
        usersByFbId(fbUserIdsToInsert).toMap
    
    val invitationsToInsert =
      for(u <- (z ++ usersAlreadyInSystem) if ! alreadyParticipantUserIds.contains(u._1))
        yield ParticipationInvitation(
          decisionId = r.decisionId,
          facebookAppRequestId = r.request,
          invitedUserId = u._1,
          invitingUserId = invitingUserId)    

    logger.debug("invitationsToInsert : " + invitationsToInsert)

    participationInvitations.insert(invitationsToInsert)

    // automatic acceptation : 
    val dps = 
      for(i <- invitationsToInsert)
        yield DecisionParticipation(i.decisionId, i.invitedUserId)
    
    decisionParticipations.insert(dps)

/*    
    from(participationInvitations, users)((pi, u) =>
      where(pi.decisionId === r.decisionId and pi.invitedUserId === u.id)
      select(&(u.f))
    )
    //exclude_ids

*/
  }
  
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
        from(participationInvitations)(pi => 
          where(pi.facebookAppRequestId === fbReqId)
          select(pi.decisionId)
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
  
  def getBallotList(userId: Long) = transaction {
    println("123---1")
    //getBallot
    val ds = 
      from(decisionParticipations, decisions)((dp, d) =>
        where(dp.voterId === userId and dp.decisionId === d.id)
        select(d)
        orderBy(dp.lastModifTime desc)
      ).page(0, 10).toList
    
    println("123---2> " + ds)
    
    for(d <- ds)
      yield getBallot(d, userId)
  }
  
  def usersByFbId(fbIds: Traversable[Long]) =
    from(Schema.users)(u =>
      where { 
        val fbId = u.facebookId.~

        println(fbId)
        val r = fbId.in(fbIds)

        r
      }
      select(&(u.id), &(u.facebookId))
    )
    
  def lookupFacebookUser(fbId: Long) = inTransaction {
    Schema.users.where(_.facebookId === fbId).headOption
  }    
    
  //===========================================================================================
/*
  def decisionsOf(userId: Long, returnOwnedOnly : Boolean) = 
    from(decisions)(d => 
      where {
        if(returnOwnedOnly)
          d.ownerId === userId
        else 
         (d.ownerId === userId) or 
          d.id.in(from(decisionParticipations)(dp => where(dp.voterId === userId) select(dp.decisionId)))
      }
      select(d)
    ).distinct

 
  def participationSummaries(decisionIds: Seq[String]) =
    from(decisionParticipations)(dp =>
      where(dp.decisionId.in(decisionIds))
      groupBy(dp.decisionId)
      compute(count, nvl(sum(dp.abstained),0), nvl(sum(dp.hasVoted),0))
    )

  def alternativeSummary(decisionIds: Seq[String]) = 
    join(decisionAlternatives, votes.leftOuter)((a,v) => 
      where(a.decisionId.in(decisionIds))
      groupBy(a.decisionId, a.id, a.title)
      compute(sum(v.map(_.score)))
      on(a.id === v.map(_.alternativeId))
    ) map {t =>
      AlternativeSummary(t.key._1, t.key._2, t.key._3, t.measures.getOrElse(0))
    }

  
  def decisionSummariesOf(participantOrOwner: Long, returnOwnedOnly : Boolean) = inTransaction {

    val ds = decisionsOf(participantOrOwner, returnOwnedOnly).toSeq
    decisionSummaries(ds)
  }
  
  
  def decisionSummariesMostActive = inTransaction {

    val ds = 
      from(decisions)(d =>
        where(d.published === true)
        select(d)
        orderBy(d.weekActivity desc, d.allTimeActivity desc)
      ).page(0, 10).toSeq

    
    decisionSummaries(ds)
  }


  
  def isParticipant(decisionId: String, voterId: Long) =
    from(decisionParticipations)(dp =>
        where(dp.voterId === voterId and dp.decisionId === decisionId)
        select(dp.id)
    ).toList != Nil 
  
  
  def voteScreenModel(decisionId: String, voterId: Long) = inTransaction {

    val d = decisions.lookup(decisionId).get

    if(!isParticipant(decisionId, voterId))
      sys.error(voterId + " not participant in " + decisionId)

    val alts = decisionAlternatives.where(a => a.decisionId === decisionId).toList.toSeq

    val scores =
      votes.where(v => v.decisionId === decisionId and v.voterId === voterId).toSeq

    val resAlts =
      for(a <- alts)
        yield scores.find(_.alternativeId == a.id) match {
          case None    => CastedVote(a, d.middleOfRange)
          case Some(s) => CastedVote(a, s.score)
        }

    (d, resAlts)
  }
*/
  
  private implicit def tuple2Dp(t: (DecisionParticipation, User)) = 
    t._1.display(t._2)

  private implicit def tuplePi(t: (ParticipationInvitation, User)) = 
    t._1.display(t._2)
  
  def participantAndInvitation(decisionId: String, page: Int, size: Int) = inTransaction {
    (invitations(decisionId, page, size), participants(decisionId, page, size))
  }
  
  def participants(decisionId: String, page: Int, size: Int) = {

    from(decisionParticipations, users)((dp, u) =>
      where(dp.decisionId === decisionId and dp.voterId === u.id)
      select((dp, u))
    ).page(page, size).map(t => t : ParticipantDisplay).toSeq
  }
  
  def invitations(decisionId: String, page: Int, size: Int) = {

    from(participationInvitations, users)((i, u) =>
      where(i.decisionId === decisionId and i.invitedUserId === u.id)
      select((i, u))
    ).page(page, size).map(t => t : ParticipantDisplay).toSeq
  }
/*  

  def acceptOrDeclineFacebookInvitations(invitedUserId: Long, acceptOrDecline: Map[String,Boolean]) = 
    if(acceptOrDecline.isEmpty) 
      Nil 
    else transaction {

      val acceptedIds = acceptOrDecline.filter(_._2).map(_._1)
      val refusedIds  = acceptOrDecline.filterNot(_._2).map(_._1)
      val allInvitationIds = acceptOrDecline.keys

      val facebookReqIds = 
        from(participationInvitations)(dp =>
          where(dp.decisionId.in(allInvitationIds) and dp.invitedUserId === invitedUserId)
          select(&(dp.facebookAppRequestId))
        ).toList.toSeq : Seq[Long]

      /*
      FacebookProtocol.deleteAppRequest(allInvitationIds.head, invitedUserId).map { r =>
        r.status match {
          case 200 =>
            logger.debug("deleted FB apprequest" + allInvitationIds.head)
          case s:Any =>
            sys.error("delete of facebook apprequest returned with error " + r)
        }
      }
      */

      update(participationInvitations)( pi => 
        where(pi.invitedUserId === invitedUserId and pi.decisionId.in(refusedIds))
        set(pi.declined := true)
      )

      val pis = 
        participationInvitations.deleteWhere(i => i.decisionId.in(acceptedIds) and i.invitedUserId === invitedUserId)

      val toInsert = 
        for(dId <- acceptedIds)
          yield DecisionParticipation(dId, invitedUserId)

      decisionParticipations.insert(toInsert)

      facebookReqIds
    }

  def acceptFacebookInvitation(requestId: Long, invitedUserId: Long): DecisionParticipation = transaction {

    val inv = 
      participationInvitations.where((i => 
        i.facebookAppRequestId === requestId and
        i.invitedUserId === invitedUserId)
      ).head

    val n = 
      participationInvitations.deleteWhere((i => 
        i.invitedUserId === invitedUserId and 
        i.decisionId === inv.decisionId))

    decisionParticipations.insert(DecisionParticipation(inv.decisionId, inv.invitedUserId))
  }

  def pendingInvitations(userId: Long) = inTransaction {

    val pi =
      from(users, participationInvitations, decisions)((u,i,d) =>
        where(i.invitedUserId === userId and i.decisionId === d.id and i.invitingUserId === u.id)
        select(u,i,d)
      ).//remove duplicates :
      groupBy(_._2.decisionId).map { t => 

      val (u,i,d) = t._2.head

      (u.display, d)
    }
    
    pi.toSeq
  }
  
  def lookupDecision(decisionId: String) = inTransaction {
    decisions.lookup(decisionId).get
  }

  def inviteVotersFromFacebook(invitingUserId: Long, r: FBInvitationRequest) = inTransaction {

    val recipientsFacebookIds = r.to.map(_.uid).toSet

    val usersAlreadyInSystem = 
      usersByFbId(recipientsFacebookIds).toMap

    val fbUserIdsToInsert =
      recipientsFacebookIds.diff(usersAlreadyInSystem.map(_._2.get).toSet)

    val usersToInsert = 
      for(fbInfo <- r.to if fbUserIdsToInsert.contains(fbInfo.uid))
        yield User(nickName = Some(fbInfo.name), facebookId = Some(fbInfo.uid))

    logger.debug("Will insert new FB users : " + usersToInsert)
    
    users.insert(usersToInsert)

    val alreadyParticipantUserIds = 
      from(decisionParticipations)(dp =>
        where(dp.decisionId === r.decisionId and dp.voterId.in(usersAlreadyInSystem.map(_._1).toSeq))
        select(&(dp.voterId))
      ).toSet
    
    logger.debug("alreadyParticipantUserIds : " + alreadyParticipantUserIds)
    
    val z = 
      if(fbUserIdsToInsert.isEmpty)
        Map.empty
      else
        usersByFbId(fbUserIdsToInsert).toMap
    
    val invitationsToInsert =
      for(u <- (z ++ usersAlreadyInSystem) if ! alreadyParticipantUserIds.contains(u._1))
        yield ParticipationInvitation(
          decisionId = r.decisionId,
          facebookAppRequestId = r.request,
          invitedUserId = u._1,
          invitingUserId = invitingUserId)    

    logger.debug("invitationsToInsert : " + invitationsToInsert)

    participationInvitations.insert(invitationsToInsert)

  }
  
  def lookupDecisionForEdit(decisionId: String, userId: Long) = transaction {
    (decisions.where(_.id === decisionId).single,
     decisionAlternatives.where(_.decisionId === decisionId).toList.toSeq)
  }
  
  def createNewDecision(userId: Long, dp: DecisionPost) =
    dp.validate match {
      case Left(d) =>
        transaction {
          val newD = decisions.insert(d(Decision(userId, "")))
          val newAlts = dp.alternativePosts.map(a => DecisionAlternative(newD.id, a.title))
          decisionAlternatives.insert(newAlts)
          Left(newD.id)
        }
      case Right(js) => Right(js)
    }


  def editDecision(userId: Long, dp: DecisionPost) =
    dp.validate match {
      case Left(d) =>

        val (newA, existingA) = dp.alternativePosts.partition(_.id < 0)
        val aidsToValidateOwnership = existingA.map(_.id) ++ dp.choicesToDelete

        transaction {
          val existingD = decisions.where(d => Option(d.id) === dp.id).single

          assert(existingD.ownerId == userId, "not owner !")

          val illegalUpdates = 
            decisionAlternatives.where(da => da.id.in(aidsToValidateOwnership) and Option(da.decisionId) <> dp.id).
              map(da => (da.id, da.decisionId)).toMap

          assert(illegalUpdates.size > 0, 
            "Cannot update or delete a DecisionAlternative " + 
            illegalUpdates + ", they are not associated to decision " + dp.id)

          decisions.update(d(existingD))

          decisionAlternatives.deleteWhere(da => da.id in(dp.choicesToDelete))

          decisionAlternatives.insert(newA.map(a => DecisionAlternative(existingD.id, a.title)))

          decisionAlternatives.update(existingA.map(a => DecisionAlternative(existingD.id, a.title)))
          None
        }
      case Right(js) => Some(js)
    }
*/  
  def processElectionTerminations = transaction {
    
    val n = Option(new Timestamp(Util.now))
    
    update(decisions)(d =>
      where(
       d.endsOn > n and 
       d.endedByCompletionOn.isNotNull and 
       d.endedByOwnerOn.isNotNull)
      set(d.endedByCompletionOn := n)
    )
  }
    
}
