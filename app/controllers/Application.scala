package controllers

import play.api.mvc._
import lib._
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

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

}
