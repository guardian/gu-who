package lib

import org.kohsuke.github.{GHOrganization, GHUser, GHIssue}
import collection.convert.wrapAll._
import Implicits._

sealed trait StateUpdate {
  val issueCanBeClosed: Boolean
}

case object UserHasLeftOrg extends StateUpdate {
  override val issueCanBeClosed = true
}

case class MemberUserUpdate(oldProblems: Set[AccountRequirement],
                            currentProblems: Set[AccountRequirement],
                            orgMembershipWillBeConcealed: Boolean) extends StateUpdate {

  val isChange = oldProblems != currentProblems

  val issueCanBeClosed = currentProblems.isEmpty

  val worthyOfComment = issueCanBeClosed || isChange || orgMembershipWillBeConcealed

  val fixedRequirements = oldProblems -- currentProblems
}