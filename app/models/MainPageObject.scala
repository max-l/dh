package models
import com.decision_hub.FacebookOAuthManager

class MainPageObject(val isInsideFacebookCanvas: Boolean, val displayName: Option[String])(implicit m: FacebookOAuthManager) {

  def isLoggedIn = displayName.isDefined

  def facebookLogginPage = m.loginWithFacebookUrl
}
