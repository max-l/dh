package com.decision_hub

import models._
import models.Schema._
import play.api.libs.ws.WS
import org.squeryl.PrimitiveTypeMode._
import org.squeryl._
import play.api.libs.concurrent._
import com.codahale.jerkson.Json._
import java.net.URI
import java.net.URLEncoder
import com.strong_links.crypto.CryptoUtil
import controllers.JSonRestApi
import com.strong_links.crypto.CryptoField
import controllers.DecisionHubSession
import controllers.BaseDecisionHubController

object TestData {

  val random = new java.util.Random
  
  
  
   def fakeDecision(user: User, title: String, anonymous: Boolean, f: Decision => Decision, alternatives : Seq[(Option[User], String)]) = {

     val guids = JSonRestApi.newSignedGuid(secret)
     val cd = CreateDecision(guids, title, alternatives.map(_._2), false, None, None, None)
     
     val (d, _, _) = DecisionManager.newDecision(cd, user, DecisionPrivacyMode.FBAccount)
     d
  }
  
  import CryptoUtil._
  
  val secret = "X1e3?nwXg`/^?2ckh>QDWN86iEms3eCM4n_]:Gi3gWv4yc^TiuGyQn3OY>N4Sxr:" : CryptoField
  
  def fakeEmailDecision(title: String, alternatives : Seq[String]) = {
     
     val guid = JSonRestApi.newSignedGuid(secret)
     val u = User(nickName = Some("Bonzo"), email = Some("maxime.levesque@gmail.com"))
     users.insert(u)

     val guids = JSonRestApi.newSignedGuid(secret)
     val cd = CreateDecision(guids, title, alternatives, false, None, Some("maxime.levesque@gmail.com"),Some("maxou"))

     DecisionManager.newDecision(cd, u, DecisionPrivacyMode.EmailAccount)
   }
   
  case class FBPublicUserInfo(name: String, first_name: String, last_name: String)
  
  def fakeFbAccesKey(u: User, d: Decision) = {
    val s = DecisionHubSession(u.id, "", null)
    val t = pTokens.where(_.decisionId === d.id).single
    
    assert(t.userId == None)
    
    BaseDecisionHubController.accessKey(t.id, s)
  } 
  
  def fakeUsers(fbIds: Seq[Long]) =    
    Promise.sequence {
      for(i <- fbIds) yield {

        WS.url("https://graph.facebook.com/" + i).get.map { jsonResponse =>

          (i, parse[FBPublicUserInfo](jsonResponse.body))
        }
      }
    }.await(1000 * 60).get
  

   val bobFBId = 100003662792844L
   
   val nancyFBId = 100003700232195L

   def fakeData(testUserIds: Seq[Long]) = {
     
     val userInfosFromFB = fakeUsers(testUserIds)

     // insert test users always in the same order, so that their PKs are always the same :
     val users = transaction {
       userInfosFromFB.sortBy(_._1).map { t =>
            val (i, pi) = t
            Schema.users.insert(User(Some(pi.first_name), Some(pi.last_name), None, Some(i), true))
       }
     }

     val bob = users.find(_.facebookId.get == bobFBId).get

     val nancy = users.find(_.facebookId.get == nancyFBId).get
     
     val d1 = fakeDecision(bob, "The big Decision", true, identity, 
       Seq(
         None -> "1this is the one !",
         None -> "2the only absolutely correct coice !",
         None-> "3choose this if you are a winner",
         None-> "4everyone's favorite"
       ))

     val d2 = fakeDecision(bob, "Vote for the master of the universe", 
           true,
           identity,
           Seq(
         None -> "Zaza napoli",
         None -> "Yog sothoth",
         None-> "Patato Rodriguez",
         None-> "Bill Wong"
       ))

     val nancyTokD1 = fakeFbAccesKey(nancy, d1)
     val bobTokD1 = fakeFbAccesKey(bob, d1)

     
     assert(bobTokD1.decision.ownerId == bob.id)
     assert(bobTokD1.isOwnerOfDecision)
     
     assert(FacebookParticipantManager.inviteVotersFromFacebook(
         bobTokD1, 
         FBInvitationRequest(-1L, Seq(FBFriendInfo(nancy.facebookId.get, nancy.displayableName)), null : FBAuthResponse)).isLeft)

     Session.currentSession.connection.commit()

     val Seq(a1, a2, a3, a4) = 
       Schema.decisionAlternatives.where(_.decisionId === d1.id).toSeq.sortBy(_.title)
     
     val v1 = Map(
         a1.id -> 2,
         a2.id -> 0,
         a3.id -> -1,
         a4.id -> -2
     )
     
     
     
     assert(DecisionManager.vote(nancyTokD1, a1.id, v1(a1.id)).isLeft)
     assert(DecisionManager.vote(nancyTokD1, a2.id, v1(a2.id)).isLeft)
     assert(DecisionManager.vote(nancyTokD1, a3.id, v1(a3.id)).isLeft)
     assert(DecisionManager.vote(nancyTokD1, a4.id, v1(a4.id)).isLeft)
     
     val v2 = Map(
         a1.id -> 0,
         a2.id -> -2,
         a3.id -> 1,
         a4.id -> -2
     )

     assert(DecisionManager.vote(bobTokD1, a1.id, v2(a1.id)).isLeft)
     assert(DecisionManager.vote(bobTokD1, a2.id, v2(a2.id)).isLeft)
     assert(DecisionManager.vote(bobTokD1, a3.id, v2(a3.id)).isLeft)
     assert(DecisionManager.vote(bobTokD1, a4.id, v2(a4.id)).isLeft)

     DecisionManager.setDecisionPhase(bobTokD1, DecisionPhase.Ended)
     
     val d1ToValidate = DecisionManager.decisionPubicView(bobTokD1).fold(identity, msg => sys.error(msg))

     val expectedScores = 
       v1.map(_._2).zip(v2.map(_._2)).map(t => t._1 + t._2)
     
     assert(d1ToValidate.results.isDefined)
       
     assert(expectedScores.size == 4, "expected 4, got " + expectedScores.size)
     
       
     val badScores = 
       expectedScores.zip(
           d1ToValidate.results.get.sortBy(_.title).map(_.score)
       ).filter(t => t._1 != t._2)
     
     assert(badScores.isEmpty, "bad scores exist : " + badScores)

     val allDecisions = decisions.where(s => 1 === 1).toList
     assertEquals(2, allDecisions.size)

     testEmailPrivateDecision
     
     println("Success ! ")
   }
    
    
  def testEmailPrivateDecision = {
    
    val (emailDecision, publicKey, adminKey) = 
      fakeEmailDecision("Email private decision", Seq("this", "that", "or that"))

    println("AdminGUId: " + adminKey.get.id)
    println("PublicGUId: " + publicKey.id)
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
      println("-> "+r.body)
       r.body.split('=').toList match {
         case List(expiryInSeconds, value) => new AuthorizationToken(URLEncoder.encode(value,"UTF-8"), Long.MaxValue)
         case _ => sys.error("bad response " + r.body)
       }
    }.await(1000 * 30).get
  }
  
  def deleteAppRequests = {
    //curl -sL -w "%{http_code}" -X DELETE 'https://graph.facebook.com/400557276634723_100003718310868/?access_token=300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU'
  }

  def doIt {
    val appAccessToken = appToken(300426153342097L, "52242a46291a5c1d4e37b69a48be689f")

    println("TOK:" + appAccessToken)
    val testUserIds =
      WS.url("https://graph.facebook.com/300426153342097/accounts/test-users").
        withQueryString("access_token" -> appAccessToken.value).get.map { r =>

        val d = (r.json \\ "id")
        d
      }.await(1000 * 30).get.map { s =>
        java.lang.Long.parseLong(s.as[String])
      }

    Schema.initDb
    ResetSchema.doIt
    
    val usersToExclude = Set(100003718310868L)
    transaction {
      Session.currentSession.setLogger(println(_))
      fakeData(testUserIds.filterNot(uid => usersToExclude.contains(uid)))
    }
  }
  
  val testCR = """
{"linkGuids":{"adminGuid":"07ed3qWBr2eos9dk6vEF64","publicGuid":"RZe6BykT1qemi5E_Zaq4a5","guidSignatures":"BrgU92IXfYYIEl4fUfTqhvEtbKy85Ao2Hdu3UtLe/C0="},
 "isPublic":false,
 "fbAuth":{"accessToken":"AAAERPGomtJEBAObgISQW3Nhez6XmOzkZBPO7kudT52wGHQLufQpQA5Y0UeSEM1AfOkH560pZBazOlEVXLoCAw7LbG1Qh4P6oGmzuknxEs9k4hupKND","userID":"100003718310868","expiresIn":6398,"signedRequest":"AYdahvrRdveqErlR3vr6Ui1R_0cjnXW5Hy_rnsHeoJ0.eyJhbGdvcml0aG0iOiJITUFDLVNIQTI1NiIsImNvZGUiOiIyLkFRRHJjNkh6a0JFOFBVYnYuMzYwMC4xMzM5MTc4NDAwLjUtMTAwMDAzNzE4MzEwODY4fEI2S2VnMVFuRlp3RWxxY2dFdGJWMFlOaWtfTSIsImlzc3VlZF9hdCI6MTMzOTE3MjAwMiwidXNlcl9pZCI6IjEwMDAwMzcxODMxMDg2OCJ9"},
 "title":"zozo",
 "choices":[{"title":"1"},{"title":"2"},{"title":"3"}]
 }"""
    
  def main(args: Array[String]): Unit = {
     
    import play.core.StaticApplication

    new StaticApplication(new java.io.File("."))    
    doIt
    
    //com.codahale.jerkson.Json.parse[CreateDecision](testCR)
  }
}
