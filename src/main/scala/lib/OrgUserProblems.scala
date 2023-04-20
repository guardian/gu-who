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

import org.kohsuke.github.{GHIssue, GHOrganization, GHUser}
import Implicits._
import akka.stream.Materializer
import play.api.Logger
import views.html._
import com.github.nscala_time.time.Imports._
import com.gu.who.logging.Logging
import com.madgag.scalagithub.{GitHub, GitHubResponse}
import com.madgag.scalagithub.commands.{CreateComment, CreateIssue}
import com.madgag.scalagithub.model.{Issue, Org, User}
import lib.gitgithub.RichSource
import lib.model.GuWhoOrgUser

import scala.math.Ordering.Implicits._
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

case class OrgUserProblems(
  applicableRequirements: Set[AccountRequirement],
  problems: Set[AccountRequirement]
)(implicit guWhoOrgUser: GuWhoOrgUser) extends Logging {

  implicit val org = guWhoOrgUser.guWhoOrg.org
  implicit val user = guWhoOrgUser.user
  val peopleRepo = guWhoOrgUser.guWhoOrg.peopleRepo

  val schedule = TerminationSchedule.Relaxed

  lazy val applicableLabels: Set[String] = applicableRequirements.map(_.issueLabel)

  def createIssue()(implicit g: GitHub, ec: ExecutionContext) {
    require(problems.nonEmpty)

    if (org.testMembership(user)) {
      logger.info(s"Creating issue for ${user.atLogin} $problems")

      val title = s"${user.atLogin}: ${org.displayName} asks you to fix your GitHub account!"

      val issue = peopleRepo.createIssue(title, newIssueDescription)

      // goodfgodgdog

      for (p <- problems) { issue.label(p.issueLabel) }
      val createdIssue = issue.assignee(user).body(newIssueDescription).create()
      logger.info(s"Created issue #${createdIssue.getNumber} for ${user.atLogin}")
    } else {
      logger.info(s"No need to create an issue for ${user.atLogin} - they are no longer a member of the ${org.atLogin} org")
    }
  }

  def newIssueDescription: String = {
   s"""Hi ${user.atLogin} - you're a member of [${org.displayName}](${org.html_url})'s organisation on GitHub, but our
       |audit-bot has noticed your GitHub account isn't fully set up to meet the standards we need.
       |
       |If you're no longer supposed to be a member of the organisation, please use your
       |[GitHub organisation settings](https: //github.com/settings/organizations) page to leave.
       |
       |Otherwise, we just need you to help us with a few things to keep our code safe and secure on GitHub:
       |
       |${lib.StateUpdate.markdownFor(problems)}
       |
       |You have a limited amount of time to fix these issues, otherwise you 'll be automatically removed from
       |this organisation.
       |
       |Thanks for helping us out - it makes our lives a lot easier, and help keeps our code secure!
       |""".stripMargin
  }

  def updateIssue(issue: Issue)(implicit g: GitHub, ec: ExecutionContext, m: Materializer) = for {
      stateUpdate <- stateUpdateFor(issue)
  } {
    logger.info(s"Updating issue for ${org.atLogin} with $stateUpdate")

    stateUpdate match {
      case UserHasLeftOrg =>
        issue.comments2.create(CreateComment(
          s"Closing this issue, as ${user.atLogin} has left the ${org.displayName} organisation."
        ))
      case membershipTermination: MembershipTermination =>
        issue.createComment(membershipTermination.asMarkdown())
        org.remove(user)
      case update: MemberUserUpdate =>
        if (update.orgMembershipWillBeConcealed) {
          org.conceal(user)
        }

        if (update.worthyOfComment) {
          issue.createComment(update.asMarkdown())
        }

        val oldLabelSet = issue.labelNames.toSet
        val unassociatedLabels = oldLabelSet -- applicableLabels
        val newLabelSet = problems.map(_.issueLabel) ++ unassociatedLabels ++ update.terminationWarning.map(_.warnedLabel)

        if (newLabelSet != oldLabelSet) issue.setLabels(newLabelSet.toSeq: _*)
    }

    if (stateUpdate.issueCanBeClosed) {
      issue.close()
    }
  }
  
  def stateUpdateFor(issue: Issue)(implicit g: GitHub, ec: ExecutionContext, m: Materializer): Future[StateUpdate] = {
    def stateUpdateGiven(userIsMember: Boolean): Future[StateUpdate] = if (!userIsMember) Future.successful(UserHasLeftOrg) else {
      val terminationDate: Instant = schedule.terminationDateFor(issue)
      val now = Instant.now

      if (now > terminationDate) Future.successful(MembershipTermination(problems)) else for {
        labels <- issue.labels.list().all()
        userIsPublicMemberOfOrg <- org.publicMembers.check(user.login)
      } yield {
        val oldLabels: Set[String] = labels.map(_.name).toSet
        val oldProblems = oldLabels.filter(applicableLabels).map(AccountRequirements.RequirementsByLabel)

        val userShouldBeWarned = problems.nonEmpty && now > schedule.warningThresholdFor(issue)
        val userHasBeenWarned = oldLabels.contains(schedule.warnedLabel)
        val userShouldReceiveFinalWarning = userShouldBeWarned && !userHasBeenWarned

        MemberUserUpdate(
          oldProblems,
          problems,
          terminationDate,
          orgMembershipWillBeConcealed = problems.nonEmpty && userIsPublicMemberOfOrg,
          terminationWarning = Option.when(userShouldReceiveFinalWarning)(schedule)
        )
      }
    }

    for {
      userIsMember <- org.members.check(user.login)
      stateUpdate <- stateUpdateGiven(userIsMember.result)
    } yield stateUpdate
  }
}

