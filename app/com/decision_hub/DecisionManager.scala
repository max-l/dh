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

  case class DSummary(decision: Decision, numberOfVoters: Long, numberOfAbstentions: Int, numberOfVotesExcercised: Int, alternativeSummaries: Seq[AlternativeSummary])
  
  case class AlternativeSummary(decisionId: Long, alternativeId: Long, alternativeTitle: String, points: Int)
 
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

  
  def decisionSummariesOf(participantOrOwner: Long, returnOwnedOnly : Boolean) = transaction {

    val ds = decisionsOf(participantOrOwner, returnOwnedOnly).toSeq
    val decisionIds = ds.map(_.id)

    val pSums = participationSummaries(decisionIds).map(t => (t.key: Long, t.measures)).toMap

    val aSums = alternativeSummary(decisionIds).groupBy(_.decisionId)

    val dSums = ds map { d =>

      val pSum = pSums.get(d.id).getOrElse((0L,0,0))

      DSummary(
        decision = d,
        numberOfVoters = pSum._1,
        numberOfAbstentions = pSum._2,
        numberOfVotesExcercised = pSum._3,
        alternativeSummaries = aSums.get(d.id).toSeq.flatten)
    }
  }

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
          new Timestamp(System.currentTimeMillis))

    Schema.decisionParticipations.insert(participations.toSeq)
  }
}