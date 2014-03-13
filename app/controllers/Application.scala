package controllers

import play.api.mvc._
import collection.convert.wrapAll._
import lib._
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def audit(orgName: String, apiKey: String) = Action.async {
    val auditDef = AuditDef.safelyCreateFor(orgName, apiKey)

    for (orgSnapshot <- OrgSnapshot(auditDef)) yield {

      orgSnapshot.createIssuesForNewProblemUsers()

      orgSnapshot.updateExistingIssues()

      val cache = auditDef.httpResponseCache
      Logger.info("hit "+cache.getHitCount()+" net="+cache.getNetworkCount()+" req="+cache.getRequestCount())

      Ok
    }
  }

}
