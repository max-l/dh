package models

import play.libs.Json
import play.api.mvc.BodyParsers
import com.codahale.jerkson.{Json => Jerkson}
import java.sql.Timestamp
import java.util.Calendar
import com.decision_hub.FBAuthResponse


case class Guids(adminGuid: String, publicGuid: String, guidSignatures: String)

case class CreateDecision(linkGuids: Guids, title: String, choices: Seq[String], isPublic: Boolean, fbAuth: Option[FBAuthResponse], ownerEmail: Option[String], ownerName: Option[String])

case class Score(alternativeId : Long, title: String, currentScore: Option[Int])
case class Ballot(decisionId: String, decisionTitle: String, scores: Seq[Score])

case class FinalScore(title: String, score: Int, percent: Int)

case class DecisionPublicView(
    title: String, 
    owner: ParticipantDisplay,
    viewerCanVote: Boolean,
    viewerHasVoted: Boolean,
    viewerCanAdmin: Boolean,
    ownerId: Long,
    numberOfVoters: Long,
    numberOfVotesExercised: Long,
    results: Option[Seq[FinalScore]])
    
case class DSummary(decision: Decision, numberOfVoters: Long, numberOfAbstentions: Int, numberOfVotesExercised: Int, alternativeSummaries: Seq[AlternativeSummary]) {
  
  def title = decision.title
  
  def publishableStats = {
    import decision._

    val endedOn =
      endedByOwnerOn.orElse(endedByCompletionOn).orElse(endsOn.map(_.getTime))

    if(endedOn.isEmpty)
      Seq(("Results not yet public",1))
    else {
      for(a <- alternativeSummaries) 
        yield (a.alternativeTitle, a.points)
    }
  }
  
  def pieChartData = Jerkson.generate { 
    publishableStats.map(t => Seq(t._1, t._2))
  }  
}
  
case class AlternativeSummary(decisionId: String, alternativeId: Long, alternativeTitle: String, points: Int) {
  
  def percentageOfMaximumPoints(d: DSummary) =
    if(d.numberOfVoters == 0)
      0
    else
      math.floor((100 * points / (d.numberOfVoters * d.decision.voteRange))).toInt
}


case class CastedVote(alternative: DecisionAlternative, score: Int)


case class DecisionPost(
  title: String,
  id: Option[String],
  description: Option[String],
  endMode: String,
  endDate: Option[Long],
  endHour: Int,
  endMinute: Int,
  choicesToDelete: Seq[Long],
  alternativePosts: Seq[AlternativePost],
  //published: Boolean = false,
  //votesAreAnonymous: Boolean = false)
  resultsPrivateUntilEnd: Option[Boolean] = Some(false)) {

  def validate: Either[Decision => Decision,Map[String,String]] = {
    val errors = new scala.collection.mutable.ArrayBuffer[(String,String)]
    val endsOn = endMode match {
      case "time" =>
        val c = Calendar.getInstance
        c.setTimeInMillis(endDate.getOrElse {
          errors.+= ("endDate" -> "Missing end date")
          0
        })
        c.set(Calendar.HOUR_OF_DAY, endHour)
        c.set(Calendar.MINUTE, endMinute)
        Some(new Timestamp(c.getTime().getTime()))
      case "complete" => None
    }
    
    if(title.length < 5)
      errors.+= ("title" -> "Title too short")
    if(errors.size > 0)
      Right(errors.toMap)
    else
      Left((d:Decision) => d.copy(
        title = title,
        description = description,
        endsOn = endsOn
      ))
  }
}

case class AlternativePost(id: Long, title: String)


case class ParticipantDisplay(displayName: String, facebookId: Option[Long], accepted: Boolean, email: Option[String])

case class FBFriendInfo(uid: Long, name: String)

case class FBInvitationRequest(request: Long, to: Seq[FBFriendInfo], fbAuthResponse: FBAuthResponse)

object FBInvitationRequest {
  
  def testJs = 
    """
    {"request":"223983297709036",
        "to":[{"uid":"100003680681486","name":"Barbara Amcfhjfhadhf Fallerescu"},{"uid":"100003700232195","name":"Nancy Amcgkbcbaie Sharpestein"}],
        "decisionId":1}    
    """    

  def parseString(s: String) = 
    Jerkson.parse[FBInvitationRequest](s)
  
  def parse = 
    BodyParsers.parse.tolerantText.map(parseString(_))
}
