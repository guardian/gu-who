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
  def title(user: GHUser) = s"@${user.getLogin}: The Guardian asks you to fix your GitHub account!"
  def isSatisfiedBy(user: GHUser): Boolean
}

