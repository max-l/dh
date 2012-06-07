package com.decision_hub
import javax.mail._
import javax.mail.internet._
import com.sun.mail.smtp.SMTPTransport

object Mailer {


  def sendConfirmationMail(content: String, recipientAddress: String) = {

    val props = System.getProperties()
    props.put("mail.smtp.host", "smtp.webfaction.com")
    props.put("mail.debug", "true")
    props.put("mail.smtp.auth", "true");
    //props.put("mail.user", "all_clearvote@clearvote.net")
    //props.put("mail.password", "zaza123")

    val s = Session.getInstance(props);
    val m = new MimeMessage(s)
    val to = new InternetAddress(recipientAddress)

    //m.setFrom(new InternetAddress("noreply@clearvote.net"))
    m.setFrom(new InternetAddress("noreply@clearvote.net"))
    m.addRecipient(Message.RecipientType.TO, to)
    m.setSubject("Clearvote decision confirmation")
    m.setSentDate(new java.util.Date)
    m.setText(content)
    
    val transport = s.getTransport("smtp")
    try {
      transport.connect("all_clearvote", "zaza123")
      transport.sendMessage(m, m.getRecipients(Message.RecipientType.TO))
    }
    finally {
      transport.close();
    }
  }

  def main(args: Array[String]): Unit = {
    
    sendConfirmationMail("test 123", "maxime.levesque@gmail.com")
  }

}