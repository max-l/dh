
import models._
import play.api.mvc.Request
import com.decision_hub.FacebookOAuthManager

package object controllers {

  
  implicit object facebookOAuthManager extends FacebookOAuthManager(
    "300426153342097", 
    "52242a46291a5c1d4e37b69a48be689f",
    "https://localhost/fbauth")

}