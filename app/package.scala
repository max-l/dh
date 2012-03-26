
import models._
import play.api.mvc.Request
import com.decision_hub.FacebookOAuthManager

package object controllers {

  
  implicit object facebookOAuthManager extends FacebookOAuthManager(
    "300426153342097", 
    "7fd15f25798be11efb66e698f73b9aa6",
    "https://localhost/fbauth")

}