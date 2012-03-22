
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


    def dumpHeaders = {
      println("-----------------------------------------------------------")
      println(request.method + " " + request.path + request.rawQueryString)
      println(request.headers.toMap.mkString("\n"))

      println("-----------------------------------------------------------")
    }

    if(request.path.endsWith(".css") || 
       request.path.endsWith(".png") ||
       request.path.endsWith(".js"))
      super.onRouteRequest(request)
    else {
      dumpHeaders
      super.onRouteRequest(request)
    }

    
    
    
    
  }
}
