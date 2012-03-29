package models

import play.libs.Json

import com.codahale.jerkson.Json._
/*
class DecisionSummary(
  val id: Long, 
  val title: String, 
  val punchLine: String, 
  val stats: Seq[(String,Int)], 
  val percentTimeSpent: Int,
  val numberOfVoters: Int,
  val numberOfVotesExercised: Int,
  val numberOfAbstentions: Int) {

  def numberOfVotesRemaining =
    numberOfVoters - (numberOfAbstentions + numberOfVotesExercised)
  
  def hasEnded =
    numberOfVotesRemaining == 0
  
  def votePieChart = Seq(
    "Has voted" -> numberOfVotesExercised,
    "Abstensions" -> numberOfAbstentions,
    "Votes remaining" -> numberOfVotesRemaining
  )

  def pieChartData = generate {
    (stats match {
      case Nil => Seq(("Results not yet public",1))
      case _ => stats
    }).map(t => Seq(t._1, t._2))
  }

}
*/

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
  
  def pieChartData = generate { 
    publishableStats.map(t => Seq(t._1, t._2))
  }  
}
  
case class AlternativeSummary(decisionId: Long, alternativeId: Long, alternativeTitle: String, points: Int)
