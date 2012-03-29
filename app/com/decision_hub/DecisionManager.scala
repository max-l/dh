package com.decision_hub

import models.Schema._
import models._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import org.squeryl.dsl.ast.LogicalBoolean


object DecisionManager {

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
    from(decisionAlternatives, votes.leftOuter)((a,v) => 
      where(a.id === v.map(_.alternativeId) and a.decisionId.in(decisionIds))
      groupBy(a.decisionId, a.id, a.title)
      compute(sum(v.map(_.score)))
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

  def decisionSummaries(ds: Seq[Decision]) = {

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

  /**
   * scores: Seq[(alternativeId, score)]
   */
  def vote(voter: User, decision: Decision, scores: Seq[(Long,Int)]): Unit = 
    vote(voter.id, decision.id, scores)
  
  
  def vote(voterId: Long, decisionId: Long, scores: Seq[(Long,Int)]): Unit = inTransaction {

    val numRows = 
      update(decisionParticipations)(p => 
        where(p.decisionId === decisionId and p.voterId === voterId)
        set(p.hasVoted := 1)
      )
    
    val isParticipant = numRows == 1
    
    if(! isParticipant)
      sys.error("not participant !")
      
    val d = decisions.lookup(decisionId).get
    
    votes.deleteWhere(v => v.decisionId === decisionId and v.participationId === voterId)
    
    val toInsert = 
      for( (alternativeId, score) <- scores)
        yield Vote(decisionId, alternativeId, voterId, score)
    
    votes.insert(toInsert)
  }
  
  def decisionDetails(decisionId: Long) = inTransaction {

    val d = decisions.lookup(decisionId).get

    val pSums = participationSummaries(Seq(decisionId)).map(t => (t.key: Long, t.measures)).toMap

    val aSums = alternativeSummary(Seq(decisionId)).groupBy(_.decisionId)



      val pSum = pSums.get(d.id).getOrElse((0L,0,0))

      DSummary(
        decision = d,
        numberOfVoters = pSum._1,
        numberOfAbstentions = pSum._2,
        numberOfVotesExercised = pSum._3,
        alternativeSummaries = aSums.get(d.id).toSeq.flatten)

  }

  def acceptFacebookInvitation(requestIds: Iterable[Long]) = transaction {
    update(decisionParticipations)(dp =>
      where(dp.facebookAppRequestId in requestIds)
      set(
        dp.accepted := true,
        dp.timeAcceptedOrRefused := Some(new Timestamp(System.currentTimeMillis))
      )
    )
  }

  def inviteVoterFromFacebook(facebookRequestId: Long, decisionId: Long, reciptientFacebookIds: Seq[Long]) = transaction {

    val voterIds =
      from(Schema.users)(u =>
        where(u.facebookId in reciptientFacebookIds)
        select(&(u.id))
      )

    val participations = 
      for(inviteIdstr <- voterIds) 
        yield DecisionParticipation(
          decisionId,
          facebookRequestId,
          inviteIdstr, 
          new Timestamp(System.currentTimeMillis))

    Schema.decisionParticipations.insert(participations.toSeq)
  }
}