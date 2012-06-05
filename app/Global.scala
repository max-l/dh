
import play.api._
import play.api.mvc._
import models._
import org.squeryl._
import java.sql.DriverManager
import org.squeryl.adapters.PostgreSqlAdapter
import play.libs.Akka
import akka.util.Duration
import java.util.concurrent.TimeUnit
import akka.actor._
import com.decision_hub.DecisionManager

object Global extends GlobalSettings {

  
  override def beforeStart(a: Application) = {

    Schema.initDb
  }
    /*
  override def onStart(a: Application) = {

    import akka.util.Duration
    import java.util.concurrent.TimeUnit._
 
    Akka.system.scheduler.schedule(Duration(30, SECONDS), Duration(5, MINUTES)) {
      DecisionManager.processElectionTerminations
    }
  }
    */  

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {


    def dumpHeaders = {
      println("-----------------------------------------------------------")
      println(request.method + " " + request.path + request.rawQueryString)
      println(request.headers.toMap.mkString("\n"))

      println("-----------------------------------------------------------")
    }

    if(request.path.endsWith(".css") || 
       request.path.endsWith(".png") ||
       request.path.endsWith(".ico") ||
       request.path.endsWith(".js"))
      super.onRouteRequest(request)
    else {
      dumpHeaders
      super.onRouteRequest(request)
    }
  }
}
