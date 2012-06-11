package models

import play.libs.Json
import play.api.mvc.BodyParsers
import com.codahale.jerkson.{Json => Jerkson}
import java.sql.Timestamp
import java.util.Calendar
import com.decision_hub.FBAppRequestInfo

case class DecisionAlternativeM(id: Long, decisionId: String, title: String)

case class DecisionM(
  id: String, 
  title: String, 
  endsOn: Option[Timestamp], 
  automaticEnd: Boolean, 
  canInviteByEmail: Boolean, 
  mode: String, 
  phase: String,
  publicGuid: String)


case class Guids(adminGuid: String, publicGuid: String, guidSignatures: String)

case class FBAuthResponse(accessToken: String, userID: Long, expiresIn: Int, signedRequest: String)

case class CreateDecision(
    linkGuids: Guids, 
    title: String, 
    choices: Seq[String], 
    isPublic: Boolean, 
    fbAuth: Option[FBAuthResponse], 
    ownerEmail: Option[String], 
    ownerName: Option[String])

/*
{
 "linkGuids": {"adminGuid":"nBeXKjui","publicGuid":"vN","guidSignatures":"L0="},
 "isPublic":false,
 "fbAuth":{"accessToken":"AAAE","userID":"100003718310868","expiresIn":3899,"signedRequest":"4NjgifQ"},
 "title":"fghfghg",
 "choices":["dfgg","rtrtr"]
}
*/

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
    results: Option[Seq[FinalScore]], 
    publicGuid: String,
    canInviteByEmail: Boolean, 
    mode: String,
    phase: String,
    viewerCanRegister: Boolean)


case class ParticipantDisplay(displayName: String, facebookId: Option[Long], accepted: Boolean, email: Option[String], isConfirmed: Boolean)

case class FBFriendInfo(uid: Long, name: String)

case class FBInvitationRequest(request: Long, to: Seq[FBFriendInfo], fbAuthResponse: FBAuthResponse)

case class DecisionInvitationInfo(decisionTitle: String, appReqInfo: FBAppRequestInfo, decisionPublicGuid: String)
