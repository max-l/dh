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
  
  def newDecision(d: Decision) = inTransaction {
    decisions.insert(d)
  }  
  
  def decisionExists(dId: String) = inTransaction {
    decisions.lookup(dId).isDefined
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

    val title = 
      if(decision.title == "") {
        val voter = users.lookup(voterId).get
        voter.displayableName + "'s decision " + decision.creationTime
      }
      else 
        decision.title


    new Ballot(decision.id, title, resAlts)
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
  
  def voteIsComplete(decisionId: String, voterId: Long) = transaction {

    if(update(Schema.decisionParticipations)(dp =>
        where(dp.decisionId === decisionId and dp.voterId === voterId)
        set(dp.completedOn := Some(new Timestamp(System.currentTimeMillis)))
    ) != 1) sys.error("Could not mark vote as complete " + decisionId + "," + voterId)
  }
    
  def createAlternatives(decisionId: String, titles: Seq[String]) = inTransaction {
    
    val alts = titles.map { t =>
       val a = DecisionAlternative(decisionId, t)
       decisionAlternatives.insert(a).id
       a
    }
    alts
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
        yield DecisionParticipation(i.decisionId, i.invitedUserId, 0)
    
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
  
  def decisionIdsOf(userId: Long) = transaction {

    val ds = 
      from(decisionParticipations, decisions)((dp, d) =>
        where(dp.voterId === userId and dp.decisionId === d.id)
        select(d)
        orderBy(dp.lastModifTime desc)
      ).page(0, 10).toList

      for(d <- ds)
         yield Map("decisionId" -> d.id)
  }
  
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

  def decisionPubicView(decisionId: String, currentUserId: Long) = inTransaction {

    val d = decisions.lookup(decisionId).get
    val owner = users.lookup(d.ownerId).get

    val numParticipants: Long = 
      from(decisionParticipations)(dp =>
        where(dp.decisionId === decisionId)
        compute(count())
      )
      
    val numVoted = 
      from(votes)(v =>
        where(v.decisionId === decisionId)
        compute(countDistinct(v.voterId))
      ).toInt

    val viewerIsParticipant = 
      (from(decisionParticipations)(dp =>
        where(dp.decisionId === decisionId and dp.voterId === currentUserId)
        compute(count())
      ): Long) > 0

    val currentUserVotes = 
      from(votes)(v =>
        where(v.decisionId === decisionId and v.voterId === currentUserId)
        compute(countDistinct(v.voterId))
      ).toInt

    val alts = 
      if(false) //d.resultsCanBeDisplayed)
        None
      else Some(
        // participants that have not voted (no rows in votes table, don't contribute to totals)
        join(decisionAlternatives, votes.leftOuter)((a,v) => 
          where(a.decisionId === decisionId)
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
