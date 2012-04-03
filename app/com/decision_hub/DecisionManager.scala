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
    
    logger.debug("----------------1")
    
    if(!isParticipant(decisionId, voterId))
      sys.error(voterId + " not participant in " + decisionId)
    
    logger.debug("----------------2")
      
    val alts = decisionAlternatives.where(a => a.decisionId === decisionId).toList.toSeq

    val scores =
      votes.where(v => v.decisionId === decisionId and v.voterId === voterId).toSeq

    logger.debug("----------------4")
    val resAlts =
      for(a <- alts)
        yield scores.find(_.alternativeId == a.id) match {
          case None    => CastedVote(a, d.middleOfRange)
          case Some(s) => CastedVote(a, s.score)
        }

    logger.debug("----------------5")
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
  
  def decisionDetails(decisionId: Long, currentUser: Option[Long]) = inTransaction {

    val d = decisions.lookup(decisionId).get
    val pSums = participationSummaries(Seq(decisionId)).map(t => (t.key: Long, t.measures)).toMap
    val aSums = alternativeSummary(Seq(decisionId)).groupBy(_.decisionId)
    val pSum = pSums.get(d.id).getOrElse((0L,0,0))


    val isCurrentUserParticipant =
      currentUser.map { userId =>
        from(decisionParticipations)(dp =>
          where(dp.voterId === userId and dp.decisionId === decisionId)
          select(dp.id)
        ).toList != Nil
      }

    (isCurrentUserParticipant,
     DSummary(
        decision = d,
        numberOfVoters = pSum._1,
        numberOfAbstentions = pSum._2,
        numberOfVotesExercised = pSum._3,
        alternativeSummaries = aSums.get(d.id).toSeq.flatten)
     )
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