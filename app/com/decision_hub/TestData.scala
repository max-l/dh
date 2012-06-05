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

  def participationSummaries(decisionIds: Seq[String]) =
    from(decisionParticipations)(dp =>
      where(dp.decisionId.in(decisionIds))
      groupBy(dp.decisionId)
      compute(count, nvl(sum(dp.abstained),0), 1)
    )

  def alternativeSummary(decisionIds: Seq[String]) = 
    join(decisionAlternatives, votes.leftOuter)((a,v) => 
      where(a.decisionId.in(decisionIds))
      groupBy(a.decisionId, a.id, a.title)
      compute(sum(v.map(_.score)))
      on(a.id === v.map(_.alternativeId))
    ) map {t =>
      AlternativeSummary(t.key._1, t.key._2, t.key._3, t.measures.getOrElse(0))
    }
/*  
  def decisionDetails(decisionId: String, currentUser: Option[Long]) = inTransaction {

    val d = decisions.lookup(decisionId).get

    val pSums = participationSummaries(Seq(decisionId)).map(t => (t.key: String, t.measures)).toMap

    val aSums: Map[String,Iterable[AlternativeSummary]] =
      if(d.resultsCanBeDisplayed) 
        alternativeSummary(Seq(decisionId)).groupBy(_.decisionId)
      else 
        Map.empty

    val pSum = pSums.get(d.id).getOrElse((0L,0,0))
      
    
    val isCurrentUserParticipant =
      currentUser.map { userId =>
        from(decisionParticipations)(dp =>
          where(dp.voterId === userId and dp.decisionId === decisionId)
          select(dp.id)
        ).toList != Nil
      }.getOrElse(false)

    (DSummary(
        decision = d,
        numberOfVoters = pSum._1,
        numberOfAbstentions = pSum._2,
        numberOfVotesExercised = pSum._3,
        alternativeSummaries = aSums.get(d.id).toSeq.flatten),
     DecisionManager.participants(decisionId, 0, 16),
     Nil,
     isCurrentUserParticipant,
     currentUser.map(_ == d.ownerId).getOrElse(false)
    )
  }
*/  
  def decisionSummaries(ds: Seq[Decision]) =
    if(ds == Nil)
      Nil 
    else {

      val decisionIds = ds.map(_.id)
      val pSums = participationSummaries(decisionIds).map(t => (t.key: String, t.measures)).toMap
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
  
  val random = new java.util.Random
  
  
  
   def fakeDecision(user: User, title: String, anonymous: Boolean, f: Decision => Decision, alternatives : Seq[(Option[User], String)]) = {

     val guids = JSonRestApi.newSignedGuid(secret)
     val cd = CreateDecision(guids, title, alternatives.map(_._2), false, None, None, None)
     
     val (d, _, _) = DecisionManager.newDecision(cd, user, DecisionPrivacyMode.FBAccount)
     d
  }
  
  import CryptoUtil._
  
  val secret = "X1e3?nwXg`/^?2ckh>QDWN86iEms3eCM4n_]:Gi3gWv4yc^TiuGyQn3OY>N4Sxr:" : CryptoField
  
  def fakeAnonDecision(title: String, alternatives : Seq[String]) = {
     
     val guid = JSonRestApi.newSignedGuid(secret)
     val u = User(nickName = Some("Anon Owner of " + guid))
     users.insert(u)

     val guids = JSonRestApi.newSignedGuid(secret)
     val cd = CreateDecision(guids, title, alternatives, false, None, None, None)

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

     //val bobDecisions = DecisionManager.decisionSummariesOf(bob.id, true)

     val nancyTok = fakeFbAccesKey(nancy, d1)
     val bobTok = fakeFbAccesKey(bob, d1)

     
     assert(bobTok.decision.ownerId == bob.id)
     assert(bobTok.isOwnerOfDecision)
     
     assert(FacebookParticipantManager.inviteVotersFromFacebook0(
         bobTok, 
         FBInvitationRequest(d1.id,-1L, Seq(FBFriendInfo(nancy.facebookId.get, nancy.displayableName)))).isLeft)

//     FacebookParticipantManager.inviteVotersFromFacebook0(
//         adminTokenD1,
//         FBInvitationRequest(d1.id,-1L, Seq(FBFriendInfo(bob.facebookId.get, bob.displayableName))))

     Session.currentSession.connection.commit()
     
     //DecisionManager.acceptFacebookInvitation(-1L, nancy.id)
     //DecisionManager.acceptFacebookInvitation(-1L, bob.id)
     
     val Seq(a1, a2, a3, a4) = 
       Schema.decisionAlternatives.where(_.decisionId === d1.id).toSeq.sortBy(_.title)
     
     val v1 = Map(
         a1.id -> 2,
         a2.id -> 0,
         a3.id -> -1,
         a4.id -> -2
     )
     
     
     
     assert(DecisionManager.vote(nancyTok, a1.id, v1(a1.id)).isLeft)
     assert(DecisionManager.vote(nancyTok, a2.id, v1(a2.id)).isLeft)
     assert(DecisionManager.vote(nancyTok, a3.id, v1(a3.id)).isLeft)
     assert(DecisionManager.vote(nancyTok, a4.id, v1(a4.id)).isLeft)
     
     val v2 = Map(
         a1.id -> 0,
         a2.id -> -2,
         a3.id -> 1,
         a4.id -> -2
     )

     assert(DecisionManager.vote(bobTok, a1.id, v2(a1.id)).isLeft)
     assert(DecisionManager.vote(bobTok, a2.id, v2(a2.id)).isLeft)
     assert(DecisionManager.vote(bobTok, a3.id, v2(a3.id)).isLeft)
     assert(DecisionManager.vote(bobTok, a4.id, v2(a4.id)).isLeft)

     val d1ToValidate = decisionSummaries(Seq(d1)).headOption.getOrElse(sys.error(d1+ " not found in db."))

     val expectedScores = 
       v1.map(_._2).zip(v2.map(_._2)).map(t => t._1 + t._2)
     
     val badScores = 
       expectedScores.zip(
           d1ToValidate.alternativeSummaries.sortBy(da => da.alternativeTitle).map(_.points)
       ).filter(t => t._1 != t._2)
     
     assert(badScores.isEmpty, "bad scores exist : " + badScores)

     val allDecisions = decisions.where(s => 1 === 1).toList
     assertEquals(2, allDecisions.size)

     testEmailPrivateDecision
     
     println("Success ! ")
   }
    
    
  def testEmailPrivateDecision = {
    
    val (emailDecision, publicKey, adminKey) = 
      fakeAnonDecision("Email private decision", Seq("this", "that", "or that"))

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
  
  def appR = {

        def zdeleteAppRequest(requestId: Long, fbUserId: Long, appToken: String) = {
          val u = WS.url("https://graph.facebook.com/" + requestId + "_" + fbUserId + "/").
             withQueryString("access_token" -> appToken)
          u.delete
        }
        
    val appAccessToken = appToken(300426153342097L, "52242a46291a5c1d4e37b69a48be689f")

    zdeleteAppRequest(418532081508698L, 100003718310868L, appAccessToken.value)
    
    
/*    
    println("TOK:" + appAccessToken)
    //GET /fql?q=SELECT+uid2+FROM+friend+WHERE+uid1=me()&access_token=...
    val zaza =
      WS.url("https://graph.facebook.com/284195261666034").
       withQueryString("access_token" -> appAccessToken.value).get.map { r =>
        //withQueryString("q" -> "SELECT request_id, app_id FROM apprequest WHERE request_id = 338696852845604"). get.map { r =>
          println(">>>>>>>>>>> "+r.body)
      }.await(1000 * 30)
      
      //curl -sL -w "%{http_code}" 'https://graph.facebook.com/339320519466276/?access_token=300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU'
      //curl -sL -w "%{http_code}" -X DELETE 'https://graph.facebook.com/100003718310868_339320519466276/?access_token=300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU'
      
      // THis is the one !!!! :
      //curl -sL -w "%{http_code}" -X DELETE 'https://graph.facebook.com/400557276634723_100003718310868/?access_token=300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU'

      
      
      WS.url("https://graph.facebook.com/100003718310868_284195261666034").
       withQueryString("access_token" -> appAccessToken.value).delete.map { r =>
        //withQueryString("q" -> "SELECT request_id, app_id FROM apprequest WHERE request_id = 338696852845604"). get.map { r =>
          println(">>>>>>>>>>> "+r.body)
      }.await(1000 * 30)      
*/
  }
  
  
  def doIt {
    
    
    //Schema.users.posoMetaData.fieldsMetaData
    
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

    //akka.actor.ActorSystem.shutdown
    
  }
  
  def batchDel = {
        
  }
/*
  def deleteAppRequest(requestId: Long, fbUserId: Long) = {

    
    import org.apache.http.client._
    import org.apache.http.client.methods._
    import org.apache.http.params._
    
    //import org.apache.http.client.methods.

    
    //org.apache.http.client.
 
    import org.apache.http.impl.client.DefaultHttpClient

    val httpclient = new DefaultHttpClient()
    
    val u = new URI("https://graph.facebook.com/" + requestId + "_" + fbUserId + "/?" + 
        //"access_token=300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU")
        "access_token=" + URLEncoder.encode("300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU","UTF-8"))
    
    println(u)
    
    //val method = new HttpGet(u)
    val method = new HttpDelete(u)
    
    val r = httpclient.execute(method)
    
    r.getAllHeaders().mkString("\n")
    
    
  }
  
    
  def qdeleteAppRequest(requestId: Long, fbUserId: Long) = {
    val u = WS.url("https://graph.facebook.com/" + requestId + "_" + fbUserId + "/").
       withQueryString("access_token" -> URLEncoder.encode("300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU","UTF-8"))
       
     u.delete().map { r0 =>
       println(r0.status)
       println(r0.body)
      
    }.await(1000 * 5 * 60)
       
    //com.ning.http.client.
      
    
    import com.ning.http.client._;
    import java.util.concurrent.Future;

    val asyncHttpClient = new AsyncHttpClient()
    val f = asyncHttpClient.prepareDelete("https://graph.facebook.com/" + requestId + "_" + fbUserId + "/").execute();
    val r = f.get()
    
    println(r.getStatusCode())
    println(r.getStatusText())
    println(r.getResponseBody())
    
    //println(u.headers.map(h => h._1 + "=" + h._2).mkString("\n"))    
    //println("FB api call : \n" + u.url + u.queryString)
    //println(u.headers.mkString("\n"))
    //u.delete
  }
  
  def dddd = {
    deleteAppRequest(418532081508698L, 100003718310868L)
/*    
      map{ r => 
        println(r.status)
        println(r.body)
        }.await(1000 * 5 * 60)
*/      
  }
*/  
  def main(args: Array[String]): Unit = {

    //alloMaika
    //if(1 ==1) return;
   //println(FBInvitationRequest.parseString(js))
   //println(parse[FBInvitationRequest](js))
     
    import play.core.StaticApplication

   new StaticApplication(new java.io.File("."))
    
   doIt
   //appR
    //dddd
    
    //val tok = URLEncoder.encode("300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU","UTF-8")
    val tok = "300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU"
/*    
    FBBatchRequest(tok, Seq(FBBatchMethod("POST", "418532081508698_100003718310868"))).wsUri.map { r =>
      println(r.status)
      println(r.body)
    }
*/    
     
    try { 1}
    catch {
      case e:Throwable => throw e
    }
    finally {

    }
  }
}
/*
 * 
 * 
curl -F 'access_token=300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU' \
     -F 'batch=[{ "method": "POST","relative_url": "method/fql.query?query=select+name+from+user+where+uid=4"}]' https://graph.facebook.com
 
 
{"access_token":"300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU",
 "batch":[
   {"method":"DELETE","relative_url":"418532081508698_100003718310868"}
  ]
} 
 
curl -F 'access_token=300426153342097|mb3AR_FvrqBUriTeBtCwrR9gzjU' \
     -F 'batch=[{"method": "DELETE", "relative_url": "418532081508698_100003718310868"}]' https://graph.facebook.com
*/
case class FBBatchMethod(method: String, relative_url: String)


case class FBBatchRequest(access_token: String, batch: Seq[FBBatchMethod]) {
  
  def wsUri = WS.url("https://graph.facebook.com").post{ 
    val json  = com.codahale.jerkson.Json.generate(this)
    println(json)
    json
  }
}


