package models

import play.libs.Json
import play.api.mvc.BodyParsers
import com.codahale.jerkson.{Json => Jerkson}


case class DSummary(decision: Decision, numberOfVoters: Long, numberOfAbstentions: Int, numberOfVotesExercised: Int, alternativeSummaries: Seq[AlternativeSummary]) {
  
  def title = decision.title
  def punchLine = decision.punchLine 
  
  def publishableStats = {
    import decision._

    val endedOn =
      endedByOwnerOn.orElse(endedByCompletionOn).orElse(endsOn.map(_.getTime))

    if(decision.resultsPrivateUntilEnd && endedOn.isEmpty)
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
  
case class AlternativeSummary(decisionId: Long, alternativeId: Long, alternativeTitle: String, points: Int) {
  
  def percentageOfMaximumPoints(d: DSummary) =
    if(d.numberOfVoters == 0)
      0
    else
      math.floor((100 * points / (d.numberOfVoters * d.decision.voteRange))).toInt
}


case class CastedVote(alternative: DecisionAlternative, score: Int)


case class ParticipantDisplay(displayName: String, iconSrc: Option[String], accepted: Boolean)

case class FBFriendInfo(uid: Long, name: String)

case class FBInvitationRequest(decisionId: Long, request: Long, to: Seq[FBFriendInfo])

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
