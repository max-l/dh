package com.strong_links.crypto

import java.security._
import javax.crypto._
import javax.crypto.spec.SecretKeySpec

trait CryptoUtil {

  import javax.xml.bind.DatatypeConverter._
  
  def bytesToString(a: Array[Byte]) =
    printBase64Binary(a)
    
  def stringToBytes(s: String) =
    s.getBytes("UTF-8")

  def cryptoFieldFromBase64(s: String) = 
    new FullCryptoField(parseBase64Binary(s), s)

  sealed trait CryptoField {
    
    override def toString = value
    
    def value: String
    
    def rawValue: Array[Byte]

    def mapValue[A](f: String => A) = f(value)

    lazy val asLong = parseLong(value)

    def matches(f: CryptoField) = {
      val s1 = value
      val s2 = f.value
      val res = s1 == s2

       // lets code defensively :
      if(res && s1.equals(""))
        false
      else
        res
    }
  }

  sealed class FullCryptoField(val rawValue: Array[Byte], val value: String) extends CryptoField
  
  sealed class RawCryptoField(val rawValue: Array[Byte]) extends CryptoField {
    lazy val value = bytesToString(rawValue)
  }

  sealed class RefinedCryptoField(val value: String) extends CryptoField {
    lazy val rawValue = stringToBytes(value)
  }  

  implicit def stringToCryptoField(s: String) = 
    new RefinedCryptoField(s)

  implicit def longToCryptoField(l: Long) =
    new RefinedCryptoField(l.toString)
  
  implicit def byteArrayToCryptoField(a: Array[Byte]) = 
    new RawCryptoField(a)
  
  protected def parseLong(s: String) =
    try {Some(java.lang.Long.parseLong(s))}
    catch {case e:NumberFormatException => None}

  def hmacSha1(args: CryptoField*)(secret: CryptoField) = {

    val algo = createHmacSha1(secret)
    
    for(a <- args) {
      algo.update(a.rawValue)
    }
    algo.doFinal : CryptoField
  }
  
  private def createHmacSha1(secret: CryptoField) = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(secret.rawValue, "HmacSHA1"))
    mac
  }
}


  /**
   * 
   *  userName : expirationTime : encrypt(data,k) : signature
   * 
   *  where :  
   *   k is an encryption key computed by : k = HMAC(userName,expirationTime, serverSecret)
   *   signature is : HMAC( userName| expirationTime | data | antiReplayKey, k)   
   *
   * Note : 
   * 
   * 1. The antiReplayKey protects against replay attacks (it should be the SSL session Id) 
   * 2. For additional security, the cookie should have the following attributes : 
   *    HttpOnly
   *    Expires : null (session cookie)
   *    Secure  
   * 3. the 'data' field is not encrypted, in could be, by overriding methods encryptData and decryptData. 
   */


object ToughCookieStatus extends Enumeration {
  type ToughCookieStatus = Value
  val Valid, Invalid, Expired = Value
}

class ToughCookieBakery(_serverSecret: Array[Byte]) extends CryptoUtil {

  def this(_serverSecretz: String) = this(_serverSecretz.getBytes("UTF-8"))

  implicit def c2cf(cf: CryptoField) = 
    cf.value
  
  protected def dataIsConfidential = false
  private val serverSecret = _serverSecret : CryptoField

  // a separator for concatenating crypto fields in hash computations
  private val | = "|" : CryptoField

  def validate(cookie: String, antiReplayKey: String) =
    if(cookie == null) // lets be defensive ...
      (ToughCookieStatus.Invalid, None)
    else 
      cookie.split(':').toList.map(s => s : CryptoField) match {
      case List(userIdInCookie, expirationTimeInCookie, dataInCookie, signatureInCookie) => {
        if(expirationTimeInCookie.asLong == None)
          (ToughCookieStatus.Invalid, None)
        else if(expirationTimeInCookie.asLong.get < System.currentTimeMillis)
          (ToughCookieStatus.Expired, None)
        else {

          val k = 
            hmacSha1(userIdInCookie, |, expirationTimeInCookie)(serverSecret)

          val computedSinature = 
            hmacSha1(userIdInCookie, |, expirationTimeInCookie, |, decryptData(dataInCookie)(k), |, antiReplayKey)(k)

          if(signatureInCookie matches computedSinature) 
            (ToughCookieStatus.Valid, Some(expirationTimeInCookie.asLong.get, dataInCookie.value, userIdInCookie.value))
          else 
            (ToughCookieStatus.Invalid, None)
        }
      }
      case _ => (ToughCookieStatus.Invalid, None)
    }

  def bake(_userId: String, durationInSeconds: Int, antiReplayKey: String, _data: String) = {

    val userId = _userId : CryptoField
    val expiryTime =  System.currentTimeMillis + (durationInSeconds * 1000) : CryptoField
    val data = _data : CryptoField

    val k = 
      hmacSha1(userId, |, expiryTime)(serverSecret)

    val signature = 
      hmacSha1(userId, |, expiryTime, |, encryptData(data)(k), |, antiReplayKey)(k)

    Seq(userId, expiryTime, data, signature).map(_.value).mkString(":")
  }

  protected def decryptData(encryptedData: CryptoField)(key: CryptoField) =
    if(dataIsConfidential) sys.error("Confidential node non implemented.")
    else encryptedData

  protected def encryptData(data: CryptoField)(key: CryptoField) =
    if(dataIsConfidential) sys.error("Confidential node non implemented.")
    else data

}

object ToughCookieBakery {

  def smokeTest = {

    val sessionKey = "fwefe5945f3944455"
    val b = new ToughCookieBakery("454554gfdgfg835j392jf45jf34583f")
    val c = b.bake("zaza", 4*3243, sessionKey, "data")
    println(c)
    println("valid : " + b.validate(c, sessionKey)._1)
  }
  
  def microBenchmark = {
    
    smokeTest

    val sessionKey = "fwefe5945f3944455"
    val iterations = 10000
    //val t = Util.timerInSeconds(iterations) {

      val b = new ToughCookieBakery(
          "454554gfdgfg835j392jf45jf34583f4-5945f39444554gfdgfg835j392jf45jf34583f4-5945")
      val c = b.bake("maxou", 2*60, "travel", sessionKey)
      
      b.validate(c, sessionKey) match {
        case ToughCookieStatus.Valid => {}
        case _ => sys.error("!!!!")
      }
    //}

    //val validationTime = (t * 1000) / iterations
    //println("avg time of a create + validate : " + validationTime.toFloat + " ms")
  }  
  
  def main(args: Array[String]): Unit = {
    smokeTest
  }
}
