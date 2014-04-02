package controllers

import play.api.mvc._
import lib._
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import play.api.libs.ws.WS._
import play.api.libs.ws.WS
import play.api.libs.json.{JsString, JsUndefined, JsSuccess, Json}
import org.kohsuke.github.{GitHub, GHIssueState}
import scala.util.matching.Regex
import play.api.libs.oauth.{OAuth, ServiceInfo, ConsumerKey}
import java.net.URLEncoder


object Application extends Controller {

  def audit(orgName: String, apiKey: String) = Action.async {
    val auditDef = AuditDef.safelyCreateFor(orgName, apiKey)

    Logger.info(s"Asked to audit ${auditDef.orgLogin} seemLegit=${auditDef.seemsLegit}")

    if (auditDef.seemsLegit) {
      for (orgSnapshot <- OrgSnapshot(auditDef)) yield {
        Logger.info(s"availableRequirementEvaluators=${orgSnapshot.availableRequirementEvaluators} ${orgSnapshot.orgUserProblemStats}")
        orgSnapshot.createIssuesForNewProblemUsers()

        orgSnapshot.updateExistingAssignedIssues()

        orgSnapshot.closeUnassignedIssues()
        
        Ok
      }
    } else future { NotAcceptable }
  }

  def index = Action {
    Ok(views.html.index())
  }

  def signin = Action {
    val githubUrl = GithubAuthUri.uri
    Redirect(githubUrl)
  }


  def callback(code: String, state: String) = Action.async {

    val githubUrl = new GithubAccessTokenUri(code, state).uri

    WS.url(githubUrl).withHeaders(("Accept", "application/json")).post("").map { res =>
      Json.parse(res.body) \ "access_token" match {
        case JsString(accessCode) => Redirect("/home").withSession("userId" -> accessCode)
        case _ => {
          Logger.error("couldn't parse the access token from the response")
          //todo - send a message back to user
          Redirect("/")
        }
      }
    }
  }

  def home = Action { req =>
    req.session.get("userId") match {
      case Some(accessToken) => Ok(views.html.home())
      case None => Ok(views.html.index())
    }
  }
}
