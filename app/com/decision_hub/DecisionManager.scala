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
  
  def newDecision(d: Decision, ownerId: Option[Long]) = inTransaction {
    
    val oid = ownerId.getOrElse {
      val u = User(nickName = Some("name me !"))
      users.insert(u)
      u.id
    }
    
    //here we'll get a new Guid :
    val d0 = decisions.insert(d.copy(ownerId = oid))

    val t = new PToken(Util.newGuid, d0.id, oid)
    pTokens.insert(t)
    t
  }  
  
  def decisionExists(tok: PToken) = inTransaction {
    decisions.lookup(tok.decisionId).isDefined
  }
  
  def updateDecision(decision: Decision) = inTransaction {
    assert {
      update(decisions)(d =>
        where(d.id === decision.id)
        set(d.title := decision.title,
        d.endsOn := decision.endsOn)
      ) == 1
    }
    true
  }
  
  def getDecision(tok: PToken) = inTransaction {  
    decisions.lookup(tok.decisionId)
  }
  
  def getAlternatives(tok: PToken) = inTransaction {
    decisionAlternatives.where(a => a.decisionId === tok.decisionId).toList
  }
  
  def getBallot(tok: PToken): Ballot = inTransaction {

    val d = decisions.lookup(tok.decisionId).get
    
    getBallot(d, tok)
  }

  private def getBallot(decision: Decision, tok: PToken): Ballot = {
    
    val voterId = tok.userId
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

    val title = 
      if(decision.title == "") {
        val voter = users.lookup(voterId).get
        voter.displayableName + "'s decision " + decision.creationTime
      }
      else 
        decision.title


    new Ballot(tok.id, title, resAlts)
  }
  
  def vote(tok: PToken, alternativeId: Long, voterId: Long, score: Int) = inTransaction {
    
    //TODO: verify if participant
    
    val v = 
      update(votes)(v =>
        where(v.decisionId === tok.decisionId and v.alternativeId === alternativeId and v.voterId === voterId)
        set(v.score := score)
      )
      
    if(v < 1) 
      votes.insert(new Vote(tok.decisionId, alternativeId, voterId, score))
  }

  def createAlternative(tok: PToken, title: String) = inTransaction {
    
    decisionAlternatives.insert(DecisionAlternative(tok.decisionId, title))
  }
  
  def addParticipant(tok: PToken, participantUserId: Long) = inTransaction {

    val dp = DecisionParticipation(tok.decisionId, 0, 0)
    
    decisionParticipations.insert(dp)

  }
  
  def voteIsComplete(tok: PToken, voterId: Long) = transaction {

    if(update(Schema.decisionParticipations)(dp =>
        where(dp.decisionId === tok.decisionId and dp.voterId === voterId)
        set(dp.completedOn := Some(new Timestamp(System.currentTimeMillis)))
    ) != 1) sys.error("Could not mark vote as complete " + tok.decisionId + "," + voterId)
  }
    
  def createAlternatives(tok: PToken, titles: Seq[String]) = inTransaction {
    
    val alts = titles.map { t =>
       val a = DecisionAlternative(tok.decisionId, t)
       decisionAlternatives.insert(a).id
       a
    }
    alts
  }  
  
  def updateAlternative(tok: PToken, alternativeId: Long, title: String) = inTransaction {

    update(decisionAlternatives)(a =>
      where(a.id === alternativeId)
      set(a.title := title)
    )
  }
  
  def deleteAlternative(tok: PToken, alternativeId: Long) = inTransaction {

    decisionAlternatives.deleteWhere(a => a.id === alternativeId)
  }  
  
  
  def decisionIdsOf(userId: Long) = transaction {
    
    val toks = 
      from(decisionParticipations, pTokens)((dp, tok) =>
        where(tok.userId === userId and dp.decisionId === tok.decisionId and dp.voterId === userId)
        select(tok)
        orderBy(dp.lastModifTime desc)
      ).page(0, 10).toList

/*
    val ds = 
      from(decisionParticipations, decisions)((dp, d) =>
        where(dp.voterId === userId and dp.decisionId === d.id)
        select(d)
        orderBy(dp.lastModifTime desc)
      ).page(0, 10).toList
*/
      for(t <- toks)
         yield Map("decisionId" -> t.id)
         
  }
  
  def decisionPubicView(tok: PToken) = inTransaction {

    val currentUserId = tok.userId
    
    val d = decisions.lookup(tok.decisionId).get
    
    val owner = users.lookup(d.ownerId).get

    val numParticipants: Long = 
      from(decisionParticipations)(dp =>
        where(dp.decisionId === tok.decisionId)
        compute(count())
      )
      
    val numVoted = 
      from(votes)(v =>
        where(v.decisionId === tok.decisionId)
        compute(countDistinct(v.voterId))
      ).toInt

    val viewerIsParticipant = 
      (from(decisionParticipations)(dp =>
        where(dp.decisionId === tok.decisionId and dp.voterId === currentUserId)
        compute(count())
      ): Long) > 0

    val currentUserVotes = 
      from(votes)(v =>
        where(v.decisionId === tok.decisionId and v.voterId === currentUserId)
        compute(countDistinct(v.voterId))
      ).toInt

    val alts = 
      if(false) //d.resultsCanBeDisplayed)
        None
      else Some(
        // participants that have not voted (no rows in votes table, don't contribute to totals)
        join(decisionAlternatives, votes.leftOuter)((a,v) => 
          where(a.decisionId === tok.decisionId)
          groupBy(a.title)
          compute(sum(v.map(_.score)))
          orderBy(nvl(sum(v.map(_.score)),Int.MinValue) desc)
          on(a.id === v.map(_.alternativeId))
        ) map { t =>
          val minScore = numVoted * -2
          val maxScore = numVoted *  2
          val score = t.measures.getOrElse(minScore)
          val percent =
            if(maxScore == 0) 0
            else ((score + maxScore  : Double) / (maxScore * 2  : Double)) * 100
          FinalScore(t.key, score, percent.toInt)
        }
      )

    DecisionPublicView(
      title = d.title, 
      owner = owner.display,
      ownerId = d.ownerId,
      viewerCanVote = viewerIsParticipant,
      viewerHasVoted = currentUserVotes > 0,
      numberOfVoters = numParticipants,
      numberOfVotesExercised = numVoted,
      results = alts.map(_.toSeq))
  }
  
  private implicit def tuple2Dp(t: (DecisionParticipation, User)) = 
    t._1.display(t._2)


  def participants(tok: PToken, page: Int, size: Int) = {

    from(decisionParticipations, users)((dp, u) =>
      where(dp.decisionId === tok.decisionId and dp.voterId === u.id)
      select((dp, u))
    ).page(page, size).map(t => t : ParticipantDisplay).toSeq
  }

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
