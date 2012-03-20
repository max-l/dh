
import play.api._
import play.api.mvc._
import models._
import org.squeryl._
import java.sql.DriverManager
import org.squeryl.adapters.PostgreSqlAdapter

object Global extends GlobalSettings {

  override def beforeStart(a: Application) = {
    
    
    
    val envInfo = HerokuUtils.environementExtractor

    SessionFactory.concreteFactory = Some(() =>
      org.squeryl.Session.create(DriverManager.getConnection(envInfo.psqlUrl, envInfo.dbUsername, envInfo.dbPassword), new PostgreSqlAdapter))
    
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {

     println(" ------------------- " + request.toString)

     val c = request.cookies
     
     //println("session : " + request.session.toString())

     val r = super.onRouteRequest(request)

     r
  }
}
