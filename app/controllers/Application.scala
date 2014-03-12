package controllers

import play.api.mvc._
import collection.convert.wrapAll._
import lib._
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global

object GitHubAccess {
  import play.api.Play.current

  val config = play.api.Play.configuration

  val githubApiKey = config.getString("github.apikey").get

  val org = {
    val orgs = OkGitHub.conn(githubApiKey).getMyOrganizations.values
    require(orgs.size == 1, "The bot should have membership of exactly one org.")
    orgs.head
  }

  Logger.info(s"Bot org is ${org.getLogin}")

  lazy val auditDef = AuditDef(githubApiKey, org)
}

object Application extends Controller {

  def index = Action.async {
    val auditDef = GitHubAccess.auditDef

    for (orgSnapshot <- OrgSnapshot(auditDef)) yield {

      orgSnapshot.createIssuesForNewProblemUsers()

      orgSnapshot.updateExistingIssues()

      val cache = OkGitHub.cache
      Logger.info("hit "+cache.getHitCount()+" net="+cache.getNetworkCount()+" req="+cache.getRequestCount())

      Ok
    }
  }

}
