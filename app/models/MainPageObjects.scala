package models
import controllers.DecisionHubSession
import play.api.mvc.Request
import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import controllers.Secured

class MainPageObjects(val dhSession: Option[DecisionHubSession], headers: RequestHeader) {

  def isAuthenticated = 
    dhSession.isDefined
    //headers.session.get(Secured.authenticatonTokenName).isDefined

  val DEBUG = true

  private def allHeaders = 
    for(h <- headers.headers.toMap)
      yield (h._1, h._2.mkString("[",",","]"))

  def debugDump =
    if(!DEBUG) None
    else Some { 
      Seq(
        "isAuthenticated: " + isAuthenticated,
        "Method:" +  headers.method,
        "URL: " + headers.path + headers.rawQueryString,
        "Headers: \n" + allHeaders.mkString("\n")
      ).mkString("\n")
    }
}