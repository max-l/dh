package models.ev


object fr extends I18nLanguage {
  def code = "fr"
}
object en extends I18nLanguage {
  def code = "en"
}


object Languages {
  def apply(code: String) = 
    code match {
      case "fr" => fr
      case "en" => en
      case _ => sys.error("unsupported language '" + code + "'")
    }
}
import play.api.templates.Html


class I18nContent[A](val content: A, val language: I18nLanguage, val next: Option[I18nContent[A]]) {
  
  def ~(i: I18nContent[A]) = new I18nContent(i.content, i.language, Some(this))
  
  def !(implicit l: I18nLanguage): A = ?.get

  def ?(implicit l: I18nLanguage): Option[A] =
    if(language == l)
      Some(content)
    else next.map(_.!)

  def map(f: A => Html)(implicit l: I18nLanguage) = {
    val x = ?
    f(x.get)
  }

  override def toString = content.toString
}

case class Content2(title: String, _content: String) {
  def content = Html(_content)
  
  def jsContent =
    Html(_content.replace("\'","\\'").replace("\"","\\\""))
}

case class Content3(title: String, _c2: String, _c3: String) {
  def c2 = Html(_c2)
  def c3 = Html(_c3)
}

trait I18nLanguage {
  zis =>
  
  def code: String
    
  def apply(content: String) = 
    new I18nContent(content, zis, None)
    
  def apply(title: String, content: String) = 
    new I18nContent(Content2(title, content), zis, None)
  
  def custom[A](a: A) =
    new I18nContent(a, zis, None)
  
  def specific(h: Html)(implicit l: I18nLanguage) = 
    if(l == this) h 
    else Html.empty
    
  def apply(title: String, content: xml.Node) = 
    new I18nContent(Content2(title, content.toString), zis, None)
}
