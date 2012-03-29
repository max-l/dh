package com.decision_hub

import models._
import models.Schema._
import play.api.libs.ws.WS
import org.squeryl.PrimitiveTypeMode._
import org.squeryl._
import play.api.libs.concurrent._
import com.codahale.jerkson.Json._

object TestData {

   def fakeDecision(user: User, title: String, anonymous: Boolean, f: Decision => Decision, alternatives : Seq[(Option[User], String)]) = {

     val d =
      Schema.decisions.insert(f(Decision(user.id, title, title + "This is about " + title, Some("bla bla"), anonymous)))

     for(a <- alternatives) {

       decisionAlternatives.insert(DecisionAlternative(d.id, a._2, a._1.map(_.id), Some("text for " + a._2)))
     }
     
     d
  }
  
  case class FBPublicUserInfo(name: String, first_name: String, last_name: String)
  
  def fakeUsers(fbIds: Seq[Long]) =    
    Promise.sequence {
      for(i <- fbIds) yield {

        WS.url("https://graph.facebook.com/" + i).get.map { jsonResponse =>

          val pi = parse[FBPublicUserInfo](jsonResponse.body)
          transaction {
            users.insert(User(Some(pi.first_name), Some(pi.last_name), None, Some(i), None, None))
          }
        }
      }
    }.await(1000 * 60).get
  

   val bobFBId = 100003662792844L
   
   val nancyFBId = 100003700232195L

   def fakeData(testUserIds: Seq[Long]) = {
     
     val users = fakeUsers(testUserIds)


     val bob = users.find(_.facebookId.get == bobFBId).get

     val nancy = users.find(_.facebookId.get == nancyFBId).get
     
     val d1 = 
       fakeDecision(bob, "The big Decision", true, 
           (d:Decision) => d.copy(resultsPrivateUntilEnd = false), 
       Seq(
         None -> "1this is the one !",
         None -> "2the only absolutely correct coice !",
         None-> "3choose this if you are a winner"
       ))

     val d2 = 
       fakeDecision(bob, "Vote for the master of the universe", 
           true,
           identity,
           Seq(
         None -> "Zaza napoli",
         None -> "Yog sothoth",
         None-> "Patato Rodriguez",
         None-> "Bill Wong"
       ))

     //val bobDecisions = DecisionManager.decisionSummariesOf(bob.id, true)

       
     DecisionManager.inviteVoterFromFacebook(-1L, d1.id, Seq(nancy.facebookId.get))
     DecisionManager.inviteVoterFromFacebook(-1L, d1.id, Seq(bob.facebookId.get))
       
     val Seq(a1, a2, a3) = 
       Schema.decisionAlternatives.where(_.decisionId === d1.id).toSeq.sortBy(_.title)
     
     DecisionManager.vote(nancy, d1, Seq(
         a1.id -> 8,
         a2.id -> 1,
         a3.id -> 3
     ))
     
     DecisionManager.vote(bob, d1, Seq(
         a1.id -> 1,
         a2.id -> 7,
         a3.id -> 5
     ))     
     
     val d1ToValidate = DecisionManager.decisionSummaries(Seq(d1)).headOption.getOrElse(sys.error(d1+ " not found in db."))
     
     assert(d1ToValidate.numberOfVotesExercised == 2)

     val Seq(a1v, a2v, a3v) = d1ToValidate.alternativeSummaries.sortBy(da => da.alternativeTitle)
     
     assertEquals(a1v.points, (8+1))
     assertEquals(a2v.points, (1+7))
     assertEquals(a3v.points, (5+3))
     
     
     
     val allDecisions = decisions.where(s => 1 === 1).toList
     assert(2 == allDecisions.size)
     
     
     //println(" bobDecisions : " + bobDecisions)
   }
   
  def assertEquals(a1: Any, a2: Any) = {
    if(a1 != a2) sys.error("expected " + a1 + " got " + a2)
  }
  
  def appToken(clientId: Long, clientSecret: String) = {
    
    
    WS.url("https://graph.facebook.com/oauth/access_token").withQueryString(
      "grant_type"->"client_credentials",
      "grant_type" -> "client_credentials",
      "client_id" -> clientId.toString,
      "client_secret" -> clientSecret
    ).get.map { r =>
       r.body.split('=')(1)
    }.await(1000 * 30).get
  }
  
  def doIt {
    val appAccessToken = appToken(300426153342097L, "52242a46291a5c1d4e37b69a48be689f")

    println("TOK:" + appAccessToken)
    val testUserIds =
      WS.url("https://graph.facebook.com/300426153342097/accounts/test-users").
        withQueryString("access_token" -> appAccessToken). get.map { r =>

        val d = (r.json \\ "id")
        d
      }.await(1000 * 30).get.map { s =>
        java.lang.Long.parseLong(s.as[String])
      }

    Schema.initDb
    ResetSchema.doIt
    
    
    transaction {
      Session.currentSession.setLogger(println(_))
      fakeData(testUserIds)
    }

    //akka.actor.ActorSystem.shutdown
    
  }
  
  def main(args: Array[String]): Unit = {
    doIt
    
    try { 1}
    catch {
      case e:Throwable => throw e
    }
    finally {

    }
  }
}
