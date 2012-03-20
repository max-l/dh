package models

import org.squeryl.PrimitiveTypeMode._

import org.squeryl._


object Schema extends org.squeryl.Schema {

  val decisions = table[Decision]
}


trait DecisionHubEntity extends KeyedEntity[Long] {
  val id = 0L
}

class User(
   firstName: Option[String],
   lastName: Option[String],
   nickName: Option[String],
   facebookId: Option[Long],
   email: Option[String],
   passwordHash: Option[String]) extends DecisionHubEntity

case class Decision(
  ownerId: Long,
  title: String, 
  punchLine: String, 
  summary: Option[String], 
  votesAreAnonymous: Boolean) extends DecisionHubEntity 

case class DecisionAlternative(
  decisionId: Long, 
  title: String, 
  text: Option[String]) extends DecisionHubEntity

case class DecisionParticipation(
  decisionId: Long, 
  voterId: Long) extends DecisionHubEntity

case class Vote(
  decisionId: Long, 
  alternativeId: Long, 
  participationId: Long, 
  score: Int) extends DecisionHubEntity
