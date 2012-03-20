package controllers


import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.validation.Constraints._
import com.strong_links.crypto.ToughCookieBakery

object Application extends Controller with Secured {


  import views._

  import views.html.helper._  
  
  
  
    
  val o = options("" -> "")  
  
  val c = routes.Application.helloResult(true, "F", "zaza", 3, Some("red"))
  
  //routes.Decisions.
  
  println("strongly typed URI -----> " + c.url)
  
  // -- Authentication

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) if password == "zaza" => true
      case _ => false 
    })
  )

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => AuthenticationSuccess(Redirect(routes.Application.index), user._1)
    )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  def index = Action {

    Ok(html.main("")(templates.Html.empty))
  }

  def showHelloForm = IsAuthenticated { username => implicit request =>
    Ok(html.index(helloForm))
  }

  def postHello = IsAuthenticated { username => implicit request =>
    helloForm.bindFromRequest.fold(
      {formWithErrors => 

        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + formWithErrors.errors.size)
        println(formWithErrors.errors.map(_.toString() + "*").mkString("[","\n","]"))
        
        BadRequest(html.index(formWithErrors))},
      {case (inOrNot, gender,name, repeat, color) =>

        Redirect(routes.Application.helloResult(inOrNot, gender, name, repeat, color))
      }
    )
  }

  val b = "b" -> boolean
  println("b-->" + b._2.constraints)
  val s = "s" -> text
  println("s-->" + s._2.constraints)
  val n = "n" -> number
  println("n-->" + n._2.constraints)

  
  def reqBoolean = Constraint[Boolean]("constraint.required") { o =>
    Valid
  }

  
  /*
  val zz = tuple( //"f" -> boolean.verifying(nonEmpty),
      "f" -> text.verifying(nonEmpty),
      "z" -> text.verifying(nonEmpty)
     )
*/
  
  //val f = tuple(b,s,n)
  //println("Constraints : " + f.constraints)
  
  
  def helloResult(inOrNot: Boolean, gender: String, name: String, repeat: Int, color: Option[String]) = 
    IsAuthenticated { username => implicit request =>
      Ok(html.hello(inOrNot, gender, name, repeat, color))
    }
    

  val helloForm = Form(
    tuple(
      "inOrNot" -> boolean,
      //"inOrNot" -> boolean.verifying("must be checked !!!", _ == true),
      "gender" -> text,
      "name" ->  (text verifying Constraints.nonEmpty : Mapping[String]),
      "repeat" -> number(min = 1, max = 100),
      "color" -> optional(text)
    ) //.verifying( _ match {case (_,_,_,_,_) => true})
  )
  
  println("CTR : " + helloForm.constraints)
}
