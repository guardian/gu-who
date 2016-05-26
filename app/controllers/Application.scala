/*
 * Copyright 2014 The Guardian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers


import com.madgag.playgithub.auth.AuthenticatedSessions.AccessToken
import lib.Implicits._
import lib._
import lib.actions.Actions._
import play.api.Logger
import play.api.Play.current
import play.api.cache.Cache
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.mvc._

import scala.collection.convert.wrapAsScala._
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def audit(orgName: String) = GitHubAuthenticatedAction.async { implicit req =>
    val auditDef = AuditDef.safelyCreateFor(orgName, req.gitHubCredentials)

    Logger.info(s"Asked to audit ${auditDef.org.atLogin}")

    auditDef.ensureSeemsLegit()

    for (orgSnapshot <- scan(auditDef)) yield {
      Ok(views.html.userPages.results(auditDef, orgSnapshot))
    }
  }

  def scan(auditDef: AuditDef) = Cache.getOrElse(auditDef.toString) {
    new Dogpile[OrgSnapshot](
      for (orgSnapshot <- OrgSnapshot(auditDef)) yield {
        Logger.info(s"availableRequirementEvaluators=${orgSnapshot.availableRequirementEvaluators} ${orgSnapshot.orgUserProblemStats}")

        if (orgSnapshot.soManyUsersHaveProblemsThatPerhapsTheGitHubAPIIsBroken) {
          Logger.error(s"usersWithProblemsCount=${orgSnapshot.usersWithProblemsCount} - it's possible the GitHub API is broken, so no action will be taken, to avoid spamming users")
        } else {
          orgSnapshot.createIssuesForNewProblemUsers()

          orgSnapshot.updateExistingAssignedIssues()

          orgSnapshot.closeUnassignedIssues()
        }
        orgSnapshot
      }
    )
  }.doAtLeastOneMore()

  def index = Action { implicit req =>
    Ok(views.html.userPages.index(apiKeyForm))
  }

  val apiKeyForm = Form("apiKey" -> of[String])

  def storeApiKey() = Action(parse.form(apiKeyForm)) { implicit req =>
    val accessToken = req.body
    Redirect("/choose-your-org").withSession(AccessToken.SessionKey -> accessToken)
  }

  def chooseYourOrg = GitHubAuthenticatedAction { implicit req =>
    val orgs = req.gitHub.getMyOrganizations.values().toList
    Ok(views.html.userPages.orgs(orgs, req.user))
  }
}
