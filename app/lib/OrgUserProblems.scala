package lib

import org.kohsuke.github.{GHIssue, GHUser, GHOrganization}
import Implicits._
import collection.convert.wrapAsScala._
import play.api.Logger
import views.html._

case class OrgUserProblems(org: GHOrganization, user: GHUser, applicableRequirements: Set[AccountRequirement], problems: Set[AccountRequirement]) {

  lazy val applicableLabels = applicableRequirements.map(_.issueLabel)

  def createIssue() {
    require(problems.nonEmpty)

    if (org.testMembership(user)) {
      Logger.info(s"Creating issue for ${user.getLogin} $problems")

      val title = s"@${user.getLogin}: ${org.displayName} asks you to fix your GitHub account!"
      val description = views.html.ghIssues.issue(user, org, problems).body

      val issue = org.peopleRepo.createIssue(title)
      for (p <- problems) { issue.label(p.issueLabel) }
      issue.assignee(user).body(description).create()
    } else {
      Logger.info(s"No need to create an issue for ${user.getLogin} - they are no longer a member of the ${org.getLogin} org")
    }
  }

  def updateIssue(issue: GHIssue) {
    val stateUpdate = stateUpdateFor(issue)
    Logger.info(s"Updating issue for ${user.getLogin} with $stateUpdate")

    stateUpdate match {
      case UserHasLeftOrg =>
        issue.comment(views.html.ghIssues.userHasLeftOrg(org, user).body)
      case update: MemberUserUpdate =>
        if (update.orgMembershipWillBeConcealed) {
          org.conceal(user)
        }

        if (update.isChange) {
          val unassociatedLabels = issue.labelNames.filterNot(applicableLabels)
          val newLabelSet = problems.map(_.issueLabel) ++ unassociatedLabels
          issue.setLabels(newLabelSet.toSeq: _*)
        }

        if (update.worthyOfComment) {
          issue.comment(views.html.ghIssues.memberUserUpdate(org, update).body)
        }
    }

    if (stateUpdate.issueCanBeClosed) {
      issue.close()
    }
  }
  
  def stateUpdateFor(issue: GHIssue): StateUpdate = {
    if (org.testMembership(user)) {
      val oldLabels = issue.getLabels.map(_.getName).toSet

      val oldBotLabels = oldLabels.filter(applicableLabels)

      val oldProblems = oldBotLabels.map(AccountRequirements.RequirementsByLabel)

      val orgMembershipWillBeConcealed = problems.nonEmpty && org.hasPublicMember(user)

      MemberUserUpdate(oldProblems, problems, orgMembershipWillBeConcealed)
    } else UserHasLeftOrg
  }
}
