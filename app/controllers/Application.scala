package controllers

import play.api.mvc._
import lib._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import play.api.libs.ws.WS
import org.kohsuke.github.GitHub
import collection.convert.wrapAsScala._
import play.api.libs.json.JsString
import scala.Some
import play.api.Logger


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

  import GithubAppConfig._
  val ghAuthUrl = s"${authUrl}?client_id=${clientId}&scope=${scope}"
  def index = Action {
    Ok(views.html.userPages.index(ghAuthUrl))
  }

  def oauthCallback(code: String) = Action.async {
    import GithubAppConfig._
    val resFT = WS.url(accessTokenUrl)
                  .withQueryString(("code", code),("client_id", clientId),("client_secret", clientSecret))
                  .withHeaders(("Accept", "application/json"))
                  .post("")

      resFT.map{ res =>
        res.json \ "access_token" match {
          case JsString(accessCode) => Redirect("/choose-your-org").withSession("userId" -> accessCode)
          case _ => Redirect("/")
        }
    }
  }

  def chooseYourOrg = Action { implicit req =>
    req.session.get("userId") match {
      case Some(accessToken) => {
        val conn = GitHub.connectUsingOAuth(accessToken)
        val orgs = conn.getMyOrganizations().values().toList
        val user = conn.getMyself
        Ok(views.html.userPages.orgs(orgs, user, accessToken))
      }
      case None => Ok(views.html.userPages.index(ghAuthUrl))
    }
  }
}
