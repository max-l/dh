package com.decision_hub

import models.Schema._
import models._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import org.squeryl.dsl.ast.LogicalBoolean
import play.api.Logger


object DecisionManager {

  def logger = Logger("application")
  
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
    )

 
  def participationSummaries(decisionIds: Seq[Long]) =
    from(decisionParticipations)(dp =>
      where(dp.decisionId.in(decisionIds))
      groupBy(dp.decisionId)
      compute(count, nvl(sum(dp.abstained),0), nvl(sum(dp.hasVoted),0))
    )

  def alternativeSummary(decisionIds: Seq[Long]) = 
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

  def decisionSummaries(ds: Seq[Decision]) =
    if(ds == Nil)
      Nil 
    else {

      val decisionIds = ds.map(_.id)
      val pSums = participationSummaries(decisionIds).map(t => (t.key: Long, t.measures)).toMap
      val aSums = alternativeSummary(decisionIds).groupBy(_.decisionId)
      ds map { d =>
  
        val pSum = pSums.get(d.id).getOrElse((0L,0,0))
  
        DSummary(
          decision = d,
          numberOfVoters = pSum._1,
          numberOfAbstentions = pSum._2,
          numberOfVotesExercised = pSum._3,
          alternativeSummaries = aSums.get(d.id).toSeq.flatten)
      }
    }
  
  def isParticipant(decisionId: Long, voterId: Long) =
    from(decisionParticipations)(dp =>
        where(dp.voterId === voterId and dp.decisionId === decisionId)
        select(dp.id)
    ).toList != Nil 
  
  
  def voteScreenModel(decisionId: Long, voterId: Long) = inTransaction {

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

  /**
   * scores: Seq[(alternativeId, score)]
   */
  def vote(voter: User, decision: Decision, scores: Map[Long,Int]): Unit = 
    vote(voter.id, decision.id, scores)
  
  
  def vote(voterId: Long, decisionId: Long, scores: Map[Long,Int]): Unit = inTransaction {

    if(scores.isEmpty)
      sys.error("empty vote.")
    
    val numRows = 
      update(decisionParticipations)(p => 
        where(p.decisionId === decisionId and p.voterId === voterId)
        set(p.hasVoted := 1)
      )
    
    val isParticipant = numRows == 1
    
    if(! isParticipant)
      sys.error("not participant !")
      
    val d = decisions.lookup(decisionId).get
    
    if(scores.values.filter(_ > d.voteRange) != Nil)
      sys.error("votes higher than allowable range submited: " + scores.mkString)
    
    votes.deleteWhere(v => v.decisionId === decisionId and v.voterId === voterId)

    val toInsert = 
      for( (alternativeId, score) <- scores)
        yield Vote(decisionId, alternativeId, voterId, score)

    votes.insert(toInsert.toList)
  }
  
  private implicit def tuple2Dp(t: (DecisionParticipation, User)) = 
    t._1.display(t._2)

  private implicit def tuplePi(t: (ParticipationInvitation, User)) = 
    t._1.display(t._2)
    
  def participants(decisionId: Long, page: Int, size: Int) = inTransaction {

    from(decisionParticipations, users)((dp, u) =>
      where(dp.decisionId === decisionId and dp.voterId === u.id)
      select((dp, u))
    ).page(page, size).map(t => t : ParticipantDisplay).toSeq
  }
  
  def invitations(decisionId: Long, page: Int, size: Int) = inTransaction {

    from(participationInvitations, users)((i, u) =>
      where(i.decisionId === decisionId and i.invitedUserId === u.id)
      select((i, u))
    ).page(page, size).map(t => t : ParticipantDisplay).toSeq
  }
  
  def decisionDetails(decisionId: Long, currentUser: Option[Long]) = inTransaction {

    val d = decisions.lookup(decisionId).get

    val pSums = participationSummaries(Seq(decisionId)).map(t => (t.key: Long, t.measures)).toMap

    val aSums: Map[Long,Iterable[AlternativeSummary]] =
      if(d.resultsCanBeDisplayed) 
        alternativeSummary(Seq(decisionId)).groupBy(_.decisionId)
      else 
        Map.empty

    val pSum = pSums.get(d.id).getOrElse((0L,0,0))
      
    
    val isCurrentUserParticipant =
      currentUser.map { userId =>
        from(decisionParticipations)(dp =>
          where(dp.voterId === userId and dp.decisionId === decisionId)
          select(dp.id)
        ).toList != Nil
      }.getOrElse(false)

    println(">>>>>>>>>>>>1"+d.ownerId)
    println(">>>>>>>>>>>>2"+currentUser)
    (DSummary(
        decision = d,
        numberOfVoters = pSum._1,
        numberOfAbstentions = pSum._2,
        numberOfVotesExercised = pSum._3,
        alternativeSummaries = aSums.get(d.id).toSeq.flatten),
     invitations(decisionId, 0, 16),
     participants(decisionId, 0, 16),
     isCurrentUserParticipant,
     currentUser.map(_ == d.ownerId).getOrElse(false)
    )
  }

  def acceptOrDeclineFacebookInvitations(invitedUserId: Long, acceptOrDecline: Map[Long,Boolean]) = transaction {
    
    val acceptedIds = acceptOrDecline.filter(_._2).map(_._1)
    val refusedIds  = acceptOrDecline.filterNot(_._2).map(_._1)
    
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
  
  def lookupDecision(decisionId: Long) = inTransaction {
    decisions.lookup(decisionId).get
  }
  
  def lookupInvitation(facebookRequestId: Long) = inTransaction {
    
   val dp = participationInvitations.where(_.facebookAppRequestId === facebookRequestId).head
   val d = decisions.lookup(dp.decisionId).get
   val u = users.lookup(dp.invitingUserId).get

   (d, u.display)
  }
  
  def usersByFbId(fbIds: Traversable[Long]) =
    from(Schema.users)(u =>
      where(u.facebookId.in(fbIds))
      select(&(u.id), &(u.facebookId))
    )

  /**
   * Idempotent for DecisionParticipation (will not add duplicates), but will add duplicates in ParticipationInvitation table,
   * duplicates from this table will be removed at invitaion acceptation time  
   */
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
/*    
    from(participationInvitations, users)((pi, u) =>
      where(pi.decisionId === r.decisionId and pi.invitedUserId === u.id)
      select(&(u.f))
    )
    //exclude_ids

*/
  }
  
  def lookupDecisionForEdit(decisionId: Long, userId: Long) = transaction {
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
          val existingD = decisions.where(_.id === dp.id).single

          assert(existingD.ownerId == userId, "not owner !")

          val illegalUpdates = 
            decisionAlternatives.where(da => da.id.in(aidsToValidateOwnership) and da.decisionId <> dp.id).
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
}
