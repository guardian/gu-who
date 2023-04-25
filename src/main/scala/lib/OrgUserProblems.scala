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

import akka.stream.Materializer
import com.gu.who.logging.Logging
import com.madgag.scalagithub.GitHub
import com.madgag.scalagithub.commands.CreateOrUpdateIssue
import com.madgag.scalagithub.model.{Issue, Label}
import lib.Implicits._
import lib.model.GuWhoOrgUser

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.math.Ordering.Implicits._

case class IssueWithLabels(issue: Issue, labels: Set[Label]) {
  val labelNames: Set[String] = labels.map(_.name)
}

case class OrgUserProblems(
  applicableRequirements: Set[AccountRequirement],
  problems: Set[AccountRequirement]
)(implicit guWhoOrgUser: GuWhoOrgUser) extends Logging {

  implicit val org = guWhoOrgUser.guWhoOrg.org
  implicit val user = guWhoOrgUser.user
  val peopleRepo = guWhoOrgUser.guWhoOrg.peopleRepo

  val schedule = TerminationSchedule.Relaxed

  lazy val applicableLabels: Set[String] = applicableRequirements.map(_.issueLabel)

  def createIssue()(implicit g: GitHub, ec: ExecutionContext): Future[Issue] = {
    require(problems.nonEmpty)

    logger.info(s"Creating issue for ${user.atLogin} $problems")
    for {
      issueResp <- peopleRepo.createIssue(
        title = s"${user.atLogin}: ${org.displayName} asks you to fix your GitHub account!",
        newIssueDescription, labels = Some(problems.map(_.issueLabel).toSeq),
        assignee = Some(user.login))
    } yield {
      val issue = issueResp.result
      logger.info(s"Created issue #${issue.number} for ${user.atLogin}")
      issue
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

  def updateIssue(issueWithLabels: IssueWithLabels)(implicit g: GitHub, ec: ExecutionContext, m: Materializer) = for {
      stateUpdate <- stateUpdateFor(issueWithLabels)
  } yield {
    logger.info(s"Updating issue for ${org.atLogin} with $stateUpdate")

    val issue = issueWithLabels.issue
    stateUpdate match {
      case UserHasLeftOrg =>
        issue.commentAndClose(s"Closing this issue, as ${user.atLogin} has left the ${org.displayName} organisation.")
      case membershipTermination: MembershipTermination =>
        issue.commentAndClose(membershipTermination.asMarkdown())
        org.members.delete(user.login)
      case update: MemberUserUpdate =>
        if (update.orgMembershipWillBeConcealed) {
          org.publicMembers.delete(user.login)
        }

        if (update.worthyOfComment) {
          issue.createComment(update.asMarkdown())
        }

        val oldLabelSet = issueWithLabels.labelNames
        val unassociatedLabels = oldLabelSet -- applicableLabels
        val newLabelSet = problems.map(_.issueLabel) ++ unassociatedLabels ++ update.terminationWarning.map(_.warnedLabel)

        if (newLabelSet != oldLabelSet)
          issue.update(CreateOrUpdateIssue(labels = Some(newLabelSet.toSeq)))
    }
  }
  
  def stateUpdateFor(issueWithLabels: IssueWithLabels)(implicit g: GitHub, ec: ExecutionContext, m: Materializer): Future[StateUpdate] = {
    def stateUpdateGiven(userIsMember: Boolean): Future[StateUpdate] = if (!userIsMember) Future.successful(UserHasLeftOrg) else {
      val terminationDate: Instant = schedule.terminationDateFor(issueWithLabels.issue)
      val now = Instant.now

      if (now > terminationDate) Future.successful(MembershipTermination(problems)) else for {
        userIsPublicMemberOfOrg <- org.publicMembers.check(user.login)
      } yield {
        val oldLabels: Set[String] = issueWithLabels.labelNames
        val oldProblems = oldLabels.filter(applicableLabels).map(AccountRequirements.RequirementsByLabel)

        val userShouldBeWarned = problems.nonEmpty && now > schedule.warningThresholdFor(issueWithLabels.issue)
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

