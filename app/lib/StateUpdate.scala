package lib

import org.kohsuke.github.{GHOrganization, GHUser, GHIssue}
import collection.convert.wrapAll._
import Implicits._
import org.joda.time.DateTime

sealed trait StateUpdate {
  val issueCanBeClosed: Boolean
}

case object UserHasLeftOrg extends StateUpdate {
  override val issueCanBeClosed = true
}

case class MemberUserUpdate(oldProblems: Set[AccountRequirement],
                            currentProblems: Set[AccountRequirement],
                            terminationDate: DateTime,
                            orgMembershipWillBeConcealed: Boolean,
                            terminationWarning: Option[TerminationSchedule]) extends StateUpdate {

  val isChange = oldProblems != currentProblems

  val issueCanBeClosed = currentProblems.isEmpty

  val userShouldReceiveFinalWarning = terminationWarning.isDefined

  val worthyOfComment = issueCanBeClosed || isChange || orgMembershipWillBeConcealed || userShouldReceiveFinalWarning

  val fixedRequirements = oldProblems -- currentProblems
}

case class MembershipTermination(problems: Set[AccountRequirement]) extends StateUpdate {
  override val issueCanBeClosed = true
}