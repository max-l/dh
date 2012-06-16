package controllers

object MVC {
  import play.api.templates.Html
  
  trait PathExp
  
  trait Eq[A,B]

  def jsString(s: String) = "'" + s.replace("\'","\\'").replace("\"","\\\"") + "'"
  
  def If(jsEqualityExpression: String)(innerBlock: Html) =
    // ` needs to be escaped in the template code
    Html(encodeFunc("`if[jsEqualityExpression][" + innerBlock.toString + "]"))

  def For[A](a: Seq[A])(b: A => Html) =
    // ` needs to be escaped in the template code
    Html(encodeFunc("`for[pathExprOf(a)][...]"))

  def %[A](dereExpr: A) = 0
  
  def encodeFunc(intermediaryJs: String) = ""
}


object V {

  //trait IronMustacheExpression[E[_]]
  //trait If[C <: IronMustacheExpression]

  case class If[E](jsEqualityExpression: String, e: E)
  case class For[E](e: E)
  case class Deref(pathExpr: String)
  case class Chunk(text: String)

  Seq(Chunk(""),
      If("viewerHasAccount", Seq(Chunk(".."), Deref("user.displayName"), Chunk(".."), Deref("user.photoPath"), Chunk(".."))),
      For("items", Seq(Chunk("..#.."), Deref("label"), Chunk(".."), Deref("size"), Chunk("..")))
  )
/*  
  def aRestFunc: SomeType = restGet(parse.json) { r =>
    anObject
  }
  
  // in the JS controller, aRestFunc is available, it's a jQuery ajax GET call (the http method is discovered, from the play router) : 
  "__aRestFunc = function(onSuccess, onError)"
*/
  

  def validationNeverCalled = {
  //Scala generated for validateion :
    
  "__.templateName(__aRestFunc)" // in .js gets translated to : 
  //  html.templateName(restGet())
  
  "__.templateName.dyn(__aRestFunc)" // not validated
  }
  
  // invalid (at least a warning) : 
  
  // 1) # in a For : For("items", Seq(Chunk("..#.."), Deref("label"), Chunk(".."), Deref("size"), Chunk("..")))
}

