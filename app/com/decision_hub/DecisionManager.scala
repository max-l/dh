package com.decision_hub

import models.Schema._
import models._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import org.squeryl.dsl.ast.LogicalBoolean
import play.api.Logger
import models.DecisionParticipation
import controllers.AccessKey


object DecisionManager {

  def logger = Logger("application")
  
  //def newDecision = inTransaction {decisions.insert(Decision(0L, ""))}
  
  def newDecision(cd: CreateDecision, u: User, mode: DecisionPrivacyMode.Value) = inTransaction {

    assert(cd.title.length > 3)
    assert(cd.choices.size > 2)

    val owner = 
      if(u.isPersisted) u
      else users.insert(u)

    val d = decisions.insert(Decision(
        owner.id,
        cd.title,
        Util.newGuid,
        mode
      ))

    cd.choices.foreach { t =>
       val a = DecisionAlternative(d.id, t)
       decisionAlternatives.insert(a)
    }
    
    //owner is always a participant
    val dp = DecisionParticipation(d.id, u.id)
    decisionParticipations.insert(dp)
        
    import DecisionPrivacyMode._

    mode match {
      case Public =>
        val adminTok = new PToken(cd.linkGuids.adminGuid, d.id, Some(u.id), true)
        val publicTok = new PToken(cd.linkGuids.publicGuid, d.id, None, true)
        pTokens.insert(publicTok)
        pTokens.insert(adminTok)
        (d, publicTok, Some(adminTok))
      case EmailAccount =>
        // we use new GUIDs for email private decisions, they are pre confirmed, the "unguessable"
        // guids will be sent by email to the owner
        val adminTok = new PToken(Util.newGuid, d.id, Some(u.id), true)
        val publicTok = new PToken(Util.newGuid, d.id, None, true)
        pTokens.insert(publicTok)
        pTokens.insert(adminTok)
        Mailer.sendConfirmationToOwner(d, cd.copy(linkGuids = Guids(adminTok.id, publicTok.id, "")))
        (d, publicTok, Some(adminTok))
      case FBAccount =>
        val publicTok = new PToken(cd.linkGuids.publicGuid, d.id, None, true)
        pTokens.insert(publicTok)
        (d, publicTok, None)
    }
  }

//  def decisionExists(k: AccessKey) = inTransaction {
//    decisions.lookup(tok.decisionId).isDefined
//  }

  def updateDecision(k: AccessKey, decision: Decision) = k.attemptAdmin(inTransaction {
    assert {
      update(decisions)(d =>
        where(d.id === k.decision.id)
        set(d.title := decision.title,
        d.endsOn := decision.endsOn)
      ) == 1
    }
    true
  })
  
  def getDecision(k: AccessKey) = k.attemptAdmin(k.decision)

  def getAlternatives(k: AccessKey) = k.attemptAdmin(inTransaction {
    decisionAlternatives.where(a => a.decisionId === k.decision.id).toList
  })
  
  def getBallot(k: AccessKey) = k.attemptVote( transaction{

    val alts = decisionAlternatives.where(a => a.decisionId === k.decision.id).toList

    val scores =
      votes.where(v => v.decisionId === k.decision.id and v.voterId === k.userId).toList

    val resAlts =
      for(a <- alts)
        yield scores.find(_.alternativeId == a.id) match {
          case None    => Score(a.id, a.title, None)
          case Some(s) => Score(a.id, a.title, Some(s.score))
        }

        


    new Ballot(k.accessGuid, k.decision.title, resAlts)
  })
  
  def vote(k: AccessKey, alternativeId: Long, score: Int) = k.attemptVote((userId:Long) => inTransaction {

    val v = 
      update(votes)(v =>
        where(v.decisionId === k.decision.id and v.alternativeId === alternativeId and v.voterId === k.userId)
        set(v.score := score)
      )
      
    if(v < 1) 
      votes.insert(new Vote(k.decision.id, alternativeId, userId, score))
  })

  def createAlternative(k: AccessKey, title: String) = k.attemptAdmin(inTransaction {
    
    decisionAlternatives.insert(DecisionAlternative(k.decision.id, title))
  })
  
  
//  def addParticipant(k: AccessKey, participantUserId: Long) = k.attemptAddParticipant(inTransaction {
//    val dp = DecisionParticipation(k.decision.id, 0)
//    decisionParticipations.insert(dp)
//  }
  
  def voteIsComplete(k: AccessKey) = k.attemptVote(transaction {

    if(update(Schema.decisionParticipations)(dp =>
        where(dp.decisionId === k.decision.id and dp.voterId === k.userId)
        set(dp.completedOn := Some(new Timestamp(System.currentTimeMillis)))
    ) != 1) sys.error("Could not mark vote as complete " + k.decision.id + "," + k.userId)
  })

  def updateAlternative(k: AccessKey, alternativeId: Long, title: String) = k.attemptAdmin(inTransaction {
    update(decisionAlternatives)(a =>
      where(a.id === alternativeId)
      set(a.title := title)
    )
  })
  
  def deleteAlternative(k: AccessKey, alternativeId: Long) = k.attemptAdmin(inTransaction {
    decisionAlternatives.deleteWhere(a => a.id === alternativeId)
  })
  
  
  def decisionIdsOf(userId: Long) = transaction {

      val toks0 = 
        from(decisionParticipations, pTokens)((dp, tok) =>
          where(tok.userId === userId and dp.decisionId === tok.decisionId and dp.voterId === userId)
          select(tok)
          orderBy(dp.lastModifTime desc)
        )

      val toks = 
        join(decisions, decisionParticipations, pTokens, pTokens.leftOuter)((d, dp, pubTok, tok) =>
          where(dp.voterId === userId)
          select(d, dp, pubTok, tok)
          orderBy(dp.lastModifTime desc)
          on(d.id === dp.decisionId,
             pubTok.decisionId === d.id,
             tok.map(_.userId).get === Some(userId))
        ).map { t =>
         val (d, dp, pubTok, tok) = t
           
           tok match {
             case None => pubTok.id
             case Some(adminOrVoteTok) => adminOrVoteTok.id
           }
        }        
  /*
      val ds = 
        from(decisionParticipations, decisions)((dp, d) =>
          where(dp.voterId === userId and dp.decisionId === d.id)
          select(d)
          orderBy(dp.lastModifTime desc)
        ).page(0, 10).toList
  */
      val res = 
        for(t <- toks)
           yield Map("decisionId" -> t)
      
      println("------------------->" + res)
      
      res
    }
  
  def decisionPubicView(k: AccessKey) = k.attemptView(inTransaction {

    val d = decisions.lookup(k.decision.id).get
    
    val owner = users.lookup(d.ownerId).get

    val numParticipants: Long = 
      from(decisionParticipations)(dp =>
        where(dp.decisionId === k.decision.id)
        compute(count())
      )
      
    val numVoted = 
      from(votes)(v =>
        where(v.decisionId === k.decision.id)
        compute(countDistinct(v.voterId))
      ).toInt
/*
    val viewerIsParticipant = 
      (from(decisionParticipations)(dp =>
        where(dp.decisionId === tok.decisionId and dp.voterId === currentUserId)
        compute(count())
      ): Long) > 0
*/
    val currentUserCanVote = k.attemptVote(Unit).isLeft
    val currentUserCanAdmin = k.attemptAdmin(Unit).isLeft
    
    val part = 
      decisionParticipations.where(dp => 
        dp.decisionId === k.decision.id and 
        dp.voterId === k.userId).headOption
/*    
    val currentUserHasVoted =
      currentUserCanVote &&
      (from(votes)(v =>
        where(v.decisionId === k.decision.id and v.voterId === k.userId)
        compute(countDistinct(v.voterId))
       ).toInt > 0)
*/
    val alts = 
      if(false) //d.resultsCanBeDisplayed)
        None
      else Some(
        // participants that have not voted (no rows in votes table, don't contribute to totals)
        join(decisionAlternatives, votes.leftOuter)((a,v) => 
          where(a.decisionId === k.decision.id)
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
      viewerCanVote = currentUserCanVote,
      viewerHasVoted = part.map(_.completedOn.isDefined).getOrElse(false),
      viewerCanAdmin = currentUserCanAdmin, 
      numberOfVoters = numParticipants,
      numberOfVotesExercised = numVoted,
      results = alts.map(_.toSeq))
  })
  
  private implicit def tuple2Dp(t: (DecisionParticipation, User)) = 
    t._1.display(t._2)


  def participants(k: AccessKey, page: Int, size: Int) = k.attemptView( transaction {

    from(decisionParticipations, users)((dp, u) =>
      where(dp.decisionId === k.decision.id and dp.voterId === u.id)
      select((dp, u))
    ).page(page, size).map(t => t : ParticipantDisplay).toList
  })

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
