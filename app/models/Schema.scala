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
  
  val participationInvitations = table[ParticipationInvitation]

  val votes = table[Vote]
  
  val persistentLogins = table[PersistentLogin]
  
  
  on(decisionParticipations)(dp => declare(
    columns(dp.decisionId, dp.voterId) are(unique)
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
  }
}

object DecisionPrivacyMode extends Enumeration {
  type DecisionPrivacyMode = Value 
  val FBAccount = Value(1, "private-fb") 
  val EmailAccount = Value(2, "private-email") 
  val Public  = Value(3, "public")
}

class PToken(val id: String, val decisionId: Long, val userId: Option[Long], val action: Option[Int] = None) extends KeyedEntity[String]

class PersistentLogin(val userId: String, val serieId: String, val token: String, val expiryTime: Long) {

  def userIdSerieId = compositeKey(userId, serieId)
  
  def this(uId: String, exp: Long) = this(uId, Util.newGuid, Util.newGuid, exp)
  
  def renew(newExpiryTime: Long) = new PersistentLogin(userId, serieId, Util.newGuid, newExpiryTime)
}


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
        displayableName, facebookId, true, this.email)
}


case class DecisionM(id: String, title: String, endsOn: Option[Timestamp], canInviteByEmail: Boolean, mode: String)
    
case class Decision(
  ownerId: Long,
  title: String,
  mode: DecisionPrivacyMode.Value,
  canInviteByEmail: Boolean,
  description: Option[String] = None,
  startedOn: Option[Timestamp] = None,
  endsOn: Option[Timestamp] = None,// if None, ends when complete
  endedByCompletionOn: Option[Timestamp] = None,
  endedByOwnerOn: Option[Timestamp] = None,
  creationTime: Option[Timestamp] = Some(new Timestamp(System.currentTimeMillis))) extends DecisionHubEntity {

  def this() = this(0L, "",DecisionPrivacyMode.Public, false)
  
  def toModel(guid: String) = DecisionM(guid, title, endsOn, canInviteByEmail, mode.toString)
  
  def resultsCanBeDisplayed = 
      endedByCompletionOn.orElse(endedByOwnerOn).isDefined ||
      endsOn.map(_.getTime > System.currentTimeMillis).getOrElse(false)
  
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
  
  def toModel(guid: String) = DecisionAlternativeM(guid, title)
}

case class DecisionAlternativeM(decisionId: String, title: String)

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
  completedOn: Option[Timestamp] = None,
  lastModifTime: Timestamp = new Timestamp(System.currentTimeMillis)) extends DecisionHubEntity with DisplayableUser {
  
  private def truncate(s: String) =
    if(s.length() > 15) s.substring(0,14) + "..."
    else s
  
  def display(u: User) = 
    new ParticipantDisplay(
        truncate(u.displayableName), u.facebookId, true, u.email)
}

case class ParticipationInvitation(
  decisionId: Long,
  facebookAppRequestId: Long, //facebook app request id
  invitedUserId: Long,
  invitingUserId: Long,
  declined: Boolean = false,
  creationTime: Timestamp = new Timestamp(System.currentTimeMillis)) 
 extends DecisionHubEntity with DisplayableUser {

  def display(u: User) = 
    new ParticipantDisplay(u.displayableName, u.facebookId, false, u.email) 
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