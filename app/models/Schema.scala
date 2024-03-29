package models

import org.squeryl.PrimitiveTypeMode._
import org.squeryl._
import java.sql.DriverManager
import org.squeryl.adapters.PostgreSqlAdapter
import java.sql.Timestamp
import play.api.Play
import play.api.Logger
import com.decision_hub.Util


object Schema extends org.squeryl.Schema {

  val users = table[User]

  val pTokens = table[PToken]
  
  val decisions = table[Decision]

  val decisionAlternatives = table[DecisionAlternative]
  
  val decisionParticipations = table[DecisionParticipation]

  val votes = table[Vote]

  on(decisionParticipations)(dp => declare(
    columns(dp.decisionId, dp.voterId) are(unique)
  ))
  
  on(votes)(v => declare(
    columns(v.decisionId, v.alternativeId, v.voterId) are(unique)
  ))
  
  def initDb = {

    val verboseSql = true //Play.current.configuration.getString("SQL_LOG_ON").map(_ == "true").getOrElse(false)
    
    val envInfo = HerokuUtils.environementExtractor

    SessionFactory.concreteFactory = Some(() => {
      val s = org.squeryl.Session.create(DriverManager.getConnection(envInfo.psqlUrl, envInfo.dbUsername, envInfo.dbPassword), new PostgreSqlAdapter)
      
      if(verboseSql) {
        s.setLogger { msg =>
          println(msg)
        }
      }
      s
    })
  }
  
  def main(args: Array[String]): Unit = {
    
    initDb
  }
}


object ResetSchema {
  
  def doIt {
    transaction {
      try { Schema.drop }
      catch {
        case e: Exception => println("could not drop schema...") 
      }
      Schema.create
    }
  }
  
  def main(args: Array[String]): Unit = {
    

    Schema.initDb
    
    doIt
  }
}

object DecisionPrivacyMode extends Enumeration {
  type DecisionPrivacyMode = Value 
  val FBAccount =    Value(0, "private-fb") 
  val EmailAccount = Value(1, "private-email") 
  val Public  =      Value(2, "public")
}

object DecisionPhase extends Enumeration {
  type DecisionPhase = Value 
  val Draft =       Value(0, "Draft") 
  val VoteStarted = Value(1, "VoteStarted") 
  val Ended  =      Value(2, "Ended")
}


class PToken(val id: String, val decisionId: Long, val userId: Option[Long], val action: Option[Int] = None) extends KeyedEntity[String]


trait DecisionHubEntity extends KeyedEntity[Long] {
  val id = 0L
}

case class User(
   firstName: Option[String] = None,
   lastName: Option[String] = None,
   nickName: Option[String] = None,
   facebookId: Option[Long] = None,
   facebookAuthorized: Boolean = false,
   email: Option[String] = None,
   passwordHash: Option[String] = None,
   anonymousId: Option[String] = None,
   confirmed: Boolean = true) extends DecisionHubEntity {

  override def toString = 
    Seq(firstName, lastName, nickName, facebookId).mkString("User(", ",", ")")

  private def validateDisplayableName = 
    nickName orElse 
    Seq(firstName, lastName).flatten.headOption orElse
    email

  def displayableName = validateDisplayableName.get
  
  /**
   * The main constraint is to have a displayable name
   */
  def validate = {

    val accountIdentifier = 
      facebookId orElse email


    (validateDisplayableName, accountIdentifier) match {
      case (None,    Some(_)) => Right("You must specify at least a first name, last name, nickname or email.")
      case (Some(n), Some(_)) => Left(n)
      case _ => Right("invalid user " + this + ".")
    }
  }
  
  //TODO: display(externalPlatformIEnum#Value)
  def display =
    new ParticipantDisplay(
        displayableName, facebookId, true, this.email, this.confirmed)
}

case class Decision(
  ownerId: Long,
  title: String,
  mode: DecisionPrivacyMode.Value,
  canInviteByEmail: Boolean,
  phase: DecisionPhase.Value = DecisionPhase.Draft,
  automaticEnd: Boolean = false,
  description: Option[String] = None,
  startedOn: Option[Timestamp] = None,
  endsOn: Option[Timestamp] = None,// if None, ends when complete
  endedOn: Option[Timestamp] = None,
  creationTime: Option[Timestamp] = Some(new Timestamp(System.currentTimeMillis))) extends DecisionHubEntity {

  def this() = this(0L, "",DecisionPrivacyMode.Public, false)
    
  def toModel(guid: String, publicGuid: String) = 
    DecisionM(
        guid, title, endsOn, automaticEnd, canInviteByEmail, 
        mode.toString, phase.toString, publicGuid)

  def resultsCanBeDisplayed = 
      endedOn.isDefined
  
  def voteRange = 4
  
  def middleOfRange = 2
  
  def lowestScore = -2

  def alternativeLabels = 
    voteRange match {
      case 4 => Map(
        0 -> "Strongly Oppose",
        1 -> "Oppose",
        2 -> "Neutral",
        3 -> "Approve",
        4 -> "Strongly Approve"
      )
    }
} 


case class DecisionAlternative(
  decisionId: Long,
  title: String) extends DecisionHubEntity {
  
  def toModel(guid: String) = DecisionAlternativeM(id, guid, title)
}
  
object DecisionParticipationStatus extends Enumeration {
  type DecisionParticipationStatus = Value 
  
  val Invited, Accepted, RefusedToAuthorizeApp = Value
}

trait DisplayableUser {
  def display(u: User): ParticipantDisplay
}

case class DecisionParticipation(
  decisionId: Long,
  voterId: Long,
  confirmed: Boolean,
  facebookRequestId: Option[Long],
  completedOn: Option[Timestamp] = None,
  lastModifTime: Timestamp = new Timestamp(System.currentTimeMillis)) extends DecisionHubEntity with DisplayableUser {
  
  private def truncate(s: String) =
    if(s.length() > 15) s.substring(0,14) + "..."
    else s
  
  def display(u: User) = 
    new ParticipantDisplay(
        truncate(u.displayableName), u.facebookId, true, u.email, confirmed)
}

case class Vote(
  decisionId: Long, 
  alternativeId: Long, 
  voterId: Long, 
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