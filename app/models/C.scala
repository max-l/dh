package models
import play.api.templates.Html


class I18nContent[A](val content: A, val language: I18nLanguage, val next: Option[I18nContent[A]]) {
  
  def ~(i: I18nContent[A]) = new I18nContent(i.content, i.language, Some(this))
  
  def !(implicit l: I18nLanguage): A = ?.get

  def ?(implicit l: I18nLanguage): Option[A] =
    if(language == l)
      Some(content)
    else next.map(_.!)
    
  override def toString = content.toString
}

case class Content2(title: String, _content: String) {
  def content = Html(_content)
  
  def jsContent =
    Html(_content.replace("\'","\\'").replace("\"","\\\""))
}

trait I18nLanguage {
  zis =>
  
  def code: String
    
  def apply(content: String) = 
    new I18nContent(content, zis, None)
    
  def apply(title: String, content: String) = 
    new I18nContent(Content2(title, content), zis, None)
  
  def specific(h: Html)(implicit l: I18nLanguage) = 
    if(l == this) h 
    else Html.empty
    
  def apply(title: String, content: xml.Node) = 
    new I18nContent(Content2(title, content.toString), zis, None)
}
