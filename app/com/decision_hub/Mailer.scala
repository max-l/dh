package com.decision_hub
import javax.mail._
import javax.mail.internet._
import com.sun.mail.smtp.SMTPTransport

object Mailer {


  def sendConfirmationMail(content: String, recipientAddress: String) = {

    //noreply@clearvote.net

    val props = System.getProperties()
    props.put("mail.smtp.host", "smtp.webfaction.com")
    props.put("mail.debug", "true")
    props.put("mail.user", "all_clearvote")
    props.put("mail.password", "zaza123")

    val s = Session.getInstance(props);
    val m = new MimeMessage(s)
    val to = new InternetAddress(recipientAddress)

    m.setFrom(new InternetAddress("noreply@clearvote.net"))
    m.addRecipient(Message.RecipientType.TO, to)
    m.setSubject("Clearvote decision confirmation")
    m.setSentDate(new java.util.Date)
    m.setText(content)
    
    val transport = s.getTransport("smtp")
    try {
      transport.connect();
    
      transport.sendMessage(m, m.getRecipients(Message.RecipientType.TO))
    }
    finally {
      transport.close();
    }

  //s.connect('smtp.webfaction.com')
  //s.login('all_clearvote','zaza123')

  }

  def main(args: Array[String]): Unit = {
    
    sendConfirmationMail("test 123", "maxime.levesque@gmail.com")
  }

}