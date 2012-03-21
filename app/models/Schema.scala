package models

import org.squeryl.PrimitiveTypeMode._
import org.squeryl._
import java.sql.DriverManager
import org.squeryl.adapters.PostgreSqlAdapter


object Schema extends org.squeryl.Schema {

  val users = table[User]

  val decisions = table[Decision]

  val decisionAlternatives = table[DecisionAlternative]

  val votes = table[Vote]
}


object ResetSchema {
  
  def main(args: Array[String]): Unit = {
    
    val envInfo = HerokuUtils.environementExtractor

    SessionFactory.concreteFactory = Some(() =>
      Session.create(DriverManager.getConnection(envInfo.psqlUrl, envInfo.dbUsername, envInfo.dbPassword), new PostgreSqlAdapter))

    transaction {
      try { Schema.drop }
      catch {
        case e: Exception => println("could not drop schema...") 
      }
      Schema.create
    }
      
  }
}


trait DecisionHubEntity extends KeyedEntity[Long] {
  val id = 0L
}

case class User(
   firstName: Option[String],
   lastName: Option[String],
   nickName: Option[String],
   facebookId: Option[Long],
   email: Option[String],
   passwordHash: Option[String]) extends DecisionHubEntity {

  override def toString = 
    Seq(firstName, lastName, nickName, facebookId).mkString("User(", ",", ")")
   
  def displayableName = 
    nickName orElse 
    Seq(firstName, lastName).flatten.headOption orElse
    email

  /**
   * The main constraint is to have a displayable name
   */
  def validate = {

    val accountIdentifier = 
      facebookId orElse email


    (displayableName, accountIdentifier) match {
      case (None,    Some(_)) => Right("You must specify at least a first name, last name, nickname or email.")
      case (Some(n), Some(_)) => Left(n)
      case _ => Right("invalid user " + this + ".")
    }
  }
}

case class Decision(
  ownerId: Long,
  title: String, 
  punchLine: String, 
  summary: Option[String], 
  votesAreAnonymous: Boolean) extends DecisionHubEntity 

object Decision {
  
  def create = Decision(0L,"","",None,true)
}
  
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


object HerokuUtils {

  def environementExtractor = new {

    //postgres://gpurqblpum:Wzd1AWnm_vEoRlIK7-k4@ec2-107-22-193-180.compute-1.amazonaws.com/gpurqblpum
     
    val herokuStypeDbUrl = System.getenv("DATABASE_URL")

    val port =
      try {
        Integer.parseInt(System.getenv("PORT"))
      }
      catch {
        case e: Exception => sys.error("could not parse PORT environement variable " + e)
      }

    private val l =
      try {
        herokuStypeDbUrl.split('/').
        filter(_ != "").
        map(_.split('@').toList).
        flatten.
        map(_.split(':').toList).
        flatten.
        toList
      }
     catch {
       case e: Exception => sys.error("bad db url : " + herokuStypeDbUrl)
     }

    val List(dbType, dbUsername, dbPassword, dbHostname, databaseName) = l

    val psqlUrl = "jdbc:postgresql://" + dbHostname + "/" + databaseName

    val psqlUrlWithUsernameAndPassword = psqlUrl + "?user=" + dbUsername+ "&password=" + dbPassword
 }
}  