
import play.api._
import play.api.mvc._

object Global extends GlobalSettings {


  override def onRouteRequest(request: RequestHeader): Option[Handler] = {

     //println("executed before every request:" + request.toString)

     val c = request.cookies
     
     //println("session : " + request.session.toString())

     val r = super.onRouteRequest(request)

     r
  }
}
