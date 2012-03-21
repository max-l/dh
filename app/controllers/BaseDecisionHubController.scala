package controllers

import models.MainPageObjects
import play.api.mvc.Request
import play.api.mvc.Controller



trait BaseDecisionHubController extends Controller {

  implicit def dhSessionToMainPageObjects(dhs: DecisionHubSession) = new MainPageObjects(Some(dhs), dhs.requestHeaders)
  
  implicit def requestToMainPageObjects(req: Request[_]) = new MainPageObjects(None, req)

}