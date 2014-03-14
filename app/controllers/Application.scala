package controllers

import play.api.mvc._
import lib._
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object Application extends Controller {

  def audit(orgName: String, apiKey: String) = Action.async {
    val auditDef = AuditDef.safelyCreateFor(orgName, apiKey)

    if (auditDef.seemsLegit) {
      for (orgSnapshot <- OrgSnapshot(auditDef)) yield {

        orgSnapshot.createIssuesForNewProblemUsers()

        orgSnapshot.updateExistingAssignedIssues()

        orgSnapshot.closeUnassignedIssues()

        val cache = auditDef.httpResponseCache
        Logger.info("hit "+cache.getHitCount()+" net="+cache.getNetworkCount()+" req="+cache.getRequestCount())

        Ok
      }
    } else future { NotAcceptable }
  }

}
