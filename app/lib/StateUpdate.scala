package lib

import org.kohsuke.github.GHIssue
import collection.convert.wrapAll._

object StateUpdate {
  def apply(issue: GHIssue, currentProblems: Set[AccountRequirement]): StateUpdate = {
    val oldLabels = issue.getLabels.map(_.getName).toSet

    val oldBotLabels = oldLabels.filter(AccountRequirements.AllLabels)

    val oldProblems = oldBotLabels.map(AccountRequirements.RequirementsByLabel)

    StateUpdate(oldProblems, currentProblems)
  }
}

case class StateUpdate(oldProblems: Set[AccountRequirement], currentProblems: Set[AccountRequirement]) {

  val isChange = oldProblems != currentProblems

  val issueCanBeClosed = currentProblems.isEmpty

  val worthyOfComment = issueCanBeClosed || isChange

  val fixedRequirements = oldProblems -- currentProblems
}