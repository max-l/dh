package controllers

import models.MainPageObjects
import play.api.mvc.Request
import play.api.mvc.Controller
import play.api.Logger



trait BaseDecisionHubController extends Controller {

  implicit def logger = Logger("application")
}