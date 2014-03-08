package lib

import org.kohsuke.github.GHUser

object AccountRequirements {

  val All = Seq(FullNameRequirement, TwoFactorAuthRequirement, SponsorRequirement)

  val RequirementsByLabel = All.map(r => r.issueLabel -> r).toMap

  val AllLabels = All.map(_.issueLabel).toSet

  def failedBy(user: GHUser): Set[AccountRequirement] = All.filterNot(_.isSatisfiedBy(user)).toSet
}

trait AccountRequirement {
  val issueLabel: String
  val fixSummary: String
  def isSatisfiedBy(user: GHUser): Boolean
}

