package com.decision_hub
import javax.mail._
import javax.mail.internet._
import com.sun.mail.smtp.SMTPTransport
import models._
import views._
import play.api.Play


object Mailer {

  lazy val domain =
    try {
      Play.current.configuration.getString("application.domainName").get
    }
    catch {
      case e:Exception => "localhost"
    }
  
  def sendConfirmationToOwner(d: Decision, cd: CreateDecision) = {
    
    val content = html.email.ownerConfirmation(d, cd, domain).toString
    
    sendMail("Clearvote Decision Confirmation", content, new InternetAddress(cd.ownerEmail.get))
  }
  
  /**
   * returns Nil if successful, of a Seq of the recipients that had errors being sent.
   */
  def sendVoterEmails(decision: Decision, choices: Seq[String], owner: User, viewGuid: String, recipientsEmailAndVoterGuid: Seq[(String, String)]) = {
    
    
    val problemEmails = 
      for((email, voterGuid) <- recipientsEmailAndVoterGuid) yield {
      
        val content = html.email.inviteEmailParticipant(decision, choices, owner, voterGuid, viewGuid, domain).toString
  
        sendMail("Invitation to vote", content, new InternetAddress(email)) match {
          case None => None
          case Some(e) => Some(email)
        }
      }
    
    problemEmails.flatten
  }
  
  def activateEmailInvitationsForOneDecision(d: Decision, activationGuid: String) = {
    
    //val content = html.activateEmailInvitationsForOneDecision(d, activationGuid).toString
    //sendConfirmationMail(content, cd.ownerEmail.get)
  }

  def sendMail(subject: String, content: String, recipientAddress: InternetAddress): Option[Exception] =
    sendMail(subject, content, Seq(recipientAddress))
  
  def sendMail(subject: String, content: String, recipientAddresses: Seq[InternetAddress]) = {

    val props = System.getProperties()
    props.put("mail.smtp.host", "smtp.webfaction.com")
    props.put("mail.debug", "true")
    props.put("mail.smtp.auth", "true");
    //props.put("mail.user", "all_clearvote@clearvote.net")
    //props.put("mail.password", "zaza123")

    val s = Session.getInstance(props);
    val m = new MimeMessage(s)

    //m.setFrom(new InternetAddress("noreply@clearvote.net"))
    m.setFrom(new InternetAddress("noreply@clearvote.net"))
    for(r <- recipientAddresses)
      m.addRecipient(Message.RecipientType.TO, r)
      
    m.setSubject(subject)
    m.setSentDate(new java.util.Date)
    m.setText(content)
    
    val transport = s.getTransport("smtp")
    try {
      transport.connect("all_clearvote", "zaza123")
      transport.sendMessage(m, m.getRecipients(Message.RecipientType.TO))
      None
    }
    catch {
      case e: Exception => Some(e)
    }
    finally {
      transport.close();
    }
  }

  def main(args: Array[String]): Unit = {
    
    sendMail("test 321", "test 123", new InternetAddress("maxime.levesque@gmail.com"))
  }

}