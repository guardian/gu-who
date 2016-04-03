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

package lib

import com.madgag.scalagithub.GitHub._
import com.madgag.scalagithub.commands.{CreateComment, CreateIssue}
import com.madgag.scalagithub.model._
import Implicits._
import collection.convert.wrapAsScala._
import play.api.Logger
import views.html._
import com.github.nscala_time.time.Imports._

import scala.concurrent.Future

case class OrgUserProblems(org: Org, user: User, applicableRequirements: Set[AccountRequirement], problems: Set[AccountRequirement]) {

  lazy val applicableLabels: Set[String] = applicableRequirements.map(_.issueLabel)

  def createIssue() {
    require(problems.nonEmpty)

    org.members.check(user.login)

    if (org.testMembership(user)) {
      Logger.info(s"Creating issue for ${user.login} $problems")

      val title = s"@${user.login}: ${org.displayName} asks you to fix your GitHub account!"
      val description = views.html.ghIssues.issue(user, org, problems).body

      val repo: Repo = ???

      repo.issues.create(
        CreateIssue(title, Some(description))
      )

      val createdIssue: Issue = ???

      createdIssue.labels.replace(problems.map(_.issueLabel).toSeq)

      //val issue = org.peopleRepo.createIssue(title)
      // for (p <- problems) { issue.label(p.issueLabel) }

      // val createdIssue = createdIssue.assignee(user).body(description).create()
      Logger.info(s"Created issue #${createdIssue.number} for ${user.login}")
    } else {
      Logger.info(s"No need to create an issue for ${user.login} - they are no longer a member of the ${org.login} org")
    }
  }

  def updateIssue(issue: Issue) {
    val stateUpdate = stateUpdateFor(issue)
    Logger.info(s"Updating issue for ${user.login} with $stateUpdate")

    stateUpdate match {
      case UserHasLeftOrg =>
        issue.comments2.create(CreateComment(views.html.ghIssues.userHasLeftOrg(org, user).body))

      case membershipTermination: MembershipTermination =>
        issue.comments2.create(CreateComment(views.html.ghIssues.membershipTermination(user, membershipTermination)(org).body))
        org.members.
        org.remove(user)
      case update: MemberUserUpdate =>
        if (update.orgMembershipWillBeConcealed) {
          org.conceal(user)
        }

        if (update.worthyOfComment) {
          issue.comment(views.html.ghIssues.memberUserUpdate(update)(org).body)
        }

        val oldLabelSet = issue.labelNames.toSet
        val unassociatedLabels = oldLabelSet -- applicableLabels
        val newLabelSet = problems.map(_.issueLabel) ++ unassociatedLabels ++ update.terminationWarning.map(_.warnedLabel)

        if (newLabelSet != oldLabelSet) {
          issue.labels.replace(newLabelSet.toSeq)
          // issue.setLabels(newLabelSet.toSeq: _*)
        }
    }

    if (stateUpdate.issueCanBeClosed) {
      issue.close()
    }
  }
  
  def stateUpdateFor(issue: Issue): Future[StateUpdate] = {
    for {
      isMember <- org.members.check(user.login)
      stateUpdate <- if (isMember) Future.successful(UserHasLeftOrg)
      else {
        for {
          oldLabels: Seq[String] <- issue.labels.list().all().map(_.map(_.name))
          isPublicMember <- org.publicMembers.check(user.login)
        } yield {
          val oldBotLabels = oldLabels.filter(applicableLabels)

          val oldProblems = oldBotLabels.map(AccountRequirements.RequirementsByLabel).toSet

          val schedule = TerminationSchedule.Relaxed

          val terminationDate = schedule.terminationDateFor(issue)

          val now = DateTime.now

          if (now > terminationDate) MembershipTermination(problems)
          else {

            val userShouldBeWarned = problems.nonEmpty && now > (terminationDate - schedule.finalWarningPeriod)

            val userHasBeenWarned = oldLabels.contains(schedule.warnedLabel)

            val userShouldReceiveFinalWarning = userShouldBeWarned && !userHasBeenWarned

            MemberUserUpdate(
              oldProblems,
              problems,
              terminationDate,
              orgMembershipWillBeConcealed = problems.nonEmpty && isPublicMember,
              terminationWarning = Some(schedule).filter(_ => userShouldReceiveFinalWarning)
            )
          }
        }
      }
    } yield stateUpdate
  }
}

