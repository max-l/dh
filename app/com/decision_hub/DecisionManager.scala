package com.decision_hub

import models.Schema._
import models._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import org.squeryl.dsl.ast.LogicalBoolean
import play.api.Logger
import models.DecisionParticipation
import controllers.AccessKey
import java.util.Date


object DecisionManager {

  def logger = Logger("application")

  def newDecision(cd: CreateDecision, u: User, mode: DecisionPrivacyMode.Value) = inTransaction {

    assert(cd.title.length > 3)
    assert(cd.choices.size >= 2)

    val owner = 
      if(u.isPersisted) u
      else users.insert(u)

    val d = decisions.insert(Decision(
        owner.id,
        cd.title,
        mode,
        u.email.isDefined
      ))

    cd.choices.foreach { t =>
       val a = DecisionAlternative(d.id, t)
       decisionAlternatives.insert(a)
    }
    
    //owner is always a participant
    val dp = DecisionParticipation(d.id, u.id, true, None)
    decisionParticipations.insert(dp)
        
    import DecisionPrivacyMode._

    mode match {
      case Public =>
        val adminTok = new PToken(cd.linkGuids.adminGuid, d.id, Some(u.id))
        val publicTok = new PToken(cd.linkGuids.publicGuid, d.id, None)
        pTokens.insert(publicTok)
        pTokens.insert(adminTok)
        (d, publicTok, Some(adminTok))
      case EmailAccount =>
        // we use new GUIDs for email private decisions, they are pre confirmed, the "unguessable"
        // guids will be sent by email to the owner
        val adminTok = new PToken(Util.newGuid, d.id, Some(u.id))
        val publicTok = new PToken(Util.newGuid, d.id, None)
        pTokens.insert(publicTok)
        pTokens.insert(adminTok)
        Mailer.sendConfirmationToOwner(d, cd.copy(linkGuids = Guids(adminTok.id, publicTok.id, "")))
        (d, publicTok, Some(adminTok))
      case FBAccount =>
        val publicTok = new PToken(cd.linkGuids.publicGuid, d.id, None)
        pTokens.insert(publicTok)
        (d, publicTok, None)
    }
  }

  def updateDecision(k: AccessKey, decision: DecisionM) = k.attemptAdmin(inTransaction {

      update(decisions)(d =>
        where(d.id === k.decision.id)
        set(d.title := decision.title,
            d.automaticEnd := decision.automaticEnd,
            d.endsOn := decision.endsOn)
      ) == 1
  })
  
  def setDecisionPhase(k: AccessKey, phase: DecisionPhase.Value) = k.attemptAdmin (transaction {
    
    assert(update(decisions)(d =>
      where(d.id === k.decision.id)
      set(d.phase := phase)
    ) == 1 )

    getResultsIfVoteInProgress(k.decision.id, phase)
  })
  
  private def getResultsIfVoteInProgress(decisionId: Long, phase: DecisionPhase.Value) =
    phase match {
        case DecisionPhase.Ended => Some {
          val numVoted = numberOfVotesSubmited(decisionId)
          decisionResults(decisionId, numVoted)
        }
        case _ => None
    }    
  
  def requestEnableOfEmailInvitations(k: AccessKey) = k.attemptAdmin (transaction {
      
    val t = new PToken(Util.newGuid, k.decision.id, k.userId, Some(1))
    pTokens.insert(t)
    
  })

  def createEmailParticipantsAndSentInvites(k: AccessKey, emailList: Set[String]) = k.attemptInvite(transaction {
    
    val existingUsers = 
      users.where(_.email in(emailList)).toSeq

    val nonExisting = 
      (emailList -- existingUsers.map(_.email.get)) map { email =>
        val nick = email.split('@')(0)
        val u = User(email = Some(email), nickName = Some(nick))
        users.insert(u)
      }
    
    val alreadyParticipant = 
      from(decisionParticipations)((dp) => 
        where(dp.id in (existingUsers.map(_.id)))
        select(dp)
      ).toSeq
    
    if(alreadyParticipant != Nil) {
      logger.debug("already participants users won't be reinvited : " + alreadyParticipant)
    }
    
    val toInvite = 
      nonExisting.toSeq ++
      existingUsers.filter(eu => ! alreadyParticipant.exists(_.id == eu.id))
      
    val choices = 
      from(decisionAlternatives)(a => 
        where(a.decisionId === k.decision.id)
        select(a.title)
      ).toSeq
    
    val owner = users.lookup(k.decision.ownerId).get
    
    val publicGuid = 
      pTokens.where(t => t.decisionId === k.decision.id and t.userId.isNull).single/*Option*/.id
      
    val recipientsAndGuids =
      toInvite.map { u =>
        
        val dp = DecisionParticipation(k.decision.id, u.id, false, None)
        decisionParticipations.insert(dp)
        val vg = new PToken(Util.newGuid, k.decision.id, Some(u.id))
        pTokens.insert(vg)
        (u.email.get, vg.id)
      }
      
    Mailer.sendVoterEmails(k.decision, choices, owner, publicGuid, recipientsAndGuids)
  })
  
  
  def getDecision(k: AccessKey) = k.attemptAdmin {
    k.decision.toModel(k.accessGuid, k.publicGuid)
  }

  def getDecisionPublicGuid(dId: Long) = inTransaction {
    pTokens.where(t => t.decisionId === dId and t.userId.isNull).single.id
  }
  
  def getAlternatives(k: AccessKey) = k.attemptAdmin(inTransaction {
    decisionAlternatives.where(a => a.decisionId === k.decision.id).map(_.toModel(k.accessGuid))
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

  def submitVote(k: AccessKey) = k.attemptVote(transaction {

    if(update(Schema.decisionParticipations)(dp =>
        where(dp.decisionId === k.decision.id and dp.voterId === k.userId)
        set(dp.completedOn := Some(new Timestamp(System.currentTimeMillis)))
    ) != 1) sys.error("Could not mark vote as complete " + k.decision.id + "," + k.userId)
    
    numberOfVotesSubmited(k.decision.id)
  })

  def confirmParticipation(k: AccessKey) = k.attemptVote((userId: Long) => transaction {
    
     decisionParticipations.update(dp => 
       where(dp.decisionId === k.decision.id and dp.voterId === userId)
       set(dp.confirmed := true)
     ) == 1
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

      val toks = 
        join(decisions, decisionParticipations, pTokens, pTokens.leftOuter)((d, dp, pubTok, personalToken) =>
          where(dp.voterId === userId and pubTok.userId.isNull)
          select(d, dp, pubTok, personalToken)
          orderBy(dp.lastModifTime desc)
          on(d.id === dp.decisionId,
             pubTok.decisionId === d.id,
             personalToken.map(_.userId).get === Some(userId))
        ).map { t =>
         val (d, dp, pubTok, personalToken) = t
           
           personalToken match {
             case None => pubTok.id
             case Some(adminOrVoteTok) => adminOrVoteTok.id
           }
        }

      val res = 
        for(t <- toks)
           yield Map("decisionId" -> t)

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

    val numVoted = numberOfVotesSubmited(k.decision.id)

    val part = 
      decisionParticipations.where(dp => 
        dp.decisionId === k.decision.id and 
        dp.voterId === k.userId).headOption

    val alts =
      d.phase match {
         case DecisionPhase.Ended => Some(decisionResults(k.decision.id, numVoted))
         case _ => None
      } 
    
    val canAdmin = k.canAdmin
    val notEnded = k.decision.phase != DecisionPhase.Ended
    val canVote = k.canVote

    DecisionPublicView(
      title = d.title, 
      owner = owner.display,
      ownerId = d.ownerId,
      viewerCanVote = canVote && k.decision.phase == DecisionPhase.VoteStarted,
      viewerHasVoted = part.map(_.completedOn.isDefined).getOrElse(false),
      viewerCanAdmin = canAdmin, 
      numberOfVoters = numParticipants,
      numberOfVotesExercised = numVoted,
      results = alts.map(_.toSeq),
      publicGuid = k.publicGuid,
      canInviteByEmail = k.decision.canInviteByEmail,
      mode = k.decision.mode.toString,
      phase = k.decision.phase.toString,
      viewerCanRegister =
        notEnded &&
        k.decision.mode == DecisionPrivacyMode.Public && (! canAdmin) && (! canVote))
  })

  private def numberOfVotesSubmited(decisionId: Long) =
      from(decisionParticipations)(dp =>
        where(dp.decisionId === decisionId and dp.completedOn.isNotNull)
        compute(count())
      ).toInt    
  
  private def decisionResults(decisionId: Long, numberOfVotesSubmited: Int) = {
        // participants that have not voted (no rows in votes table, don't contribute to totals)    
        join(decisionAlternatives, votes.leftOuter)((a,v) => 
          where(a.decisionId === decisionId)
          groupBy(a.title)
          compute(sum(v.map(_.score)))
          orderBy(nvl(sum(v.map(_.score)),Int.MinValue) desc)
          on(a.id === v.map(_.alternativeId))
        ) map { t =>
          val minScore = numberOfVotesSubmited * -2
          val maxScore = numberOfVotesSubmited *  2
          val score = t.measures.getOrElse(minScore)
          val percent =
            if(maxScore == 0) 0
            else ((score + maxScore  : Double) / (maxScore * 2  : Double)) * 100
          FinalScore(t.key, score, percent.toInt)
        }    
  }

  def participants(k: AccessKey, page: Int, size: Int) = k.attemptView( transaction {

    from(decisionParticipations, users)((dp, u) =>
      where(dp.decisionId === k.decision.id and dp.voterId === u.id)
      select((dp, u))
    ).page(page, size).map{ t => 
      val (dp, u) = t
      dp.display(u)
    }.toList
  })

  def processElectionTerminations = transaction {
    
    val n = Option(new Timestamp(Util.now))
    
    update(decisions)(d =>
      where(
       d.endsOn > n and 
       d.automaticEnd === true and
       d.endedOn.isNotNull)
      set(d.endedOn := n)
    )
  }
}
