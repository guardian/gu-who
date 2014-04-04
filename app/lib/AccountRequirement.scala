package lib

import org.kohsuke.github.{GHOrganization, GHUser}

object AccountRequirements {

  val All = Seq(FullNameRequirement, TwoFactorAuthRequirement, SponsorRequirement)

  val RequirementsByLabel = All.map(r => r.issueLabel -> r).toMap

  def applicableTo(user: GHUser)(implicit orgSnapshot: OrgSnapshot): Set[AccountRequirement] =
    All.filter(_.appliesTo(user)).toSet

  def failedBy(user: GHUser)(implicit orgSnapshot: OrgSnapshot): Set[AccountRequirement] =
    applicableTo(user).filterNot(_.isSatisfiedBy(user))
}

trait AccountRequirement {
  val issueLabel: String
  def fixSummary(implicit org: GHOrganization): String
  def appliesTo(user: GHUser)(implicit orgSnapshot: OrgSnapshot): Boolean
  def isSatisfiedBy(user: GHUser)(implicit orgSnapshot: OrgSnapshot): Boolean
}


object FullNameRequirement extends AccountRequirement {
  override val issueLabel = "FullName"
  override def fixSummary(implicit org: GHOrganization) =
    "Enter a full name in your [GitHub profile](https://github.com/settings/profile)."

  def isSatisfiedBy(user: GHUser)(implicit orgSnapshot: OrgSnapshot) =
    Option(user.getName()).map(_.length > 5).getOrElse(false)

  override def appliesTo(user: GHUser)(implicit orgSnapshot: OrgSnapshot) = true
}

// requires a 'users.txt' file in the people repo
object SponsorRequirement extends AccountRequirement {

  override val issueLabel = "Sponsor"

  override def fixSummary(implicit org: GHOrganization) =
    "Get a Pull-Request opened to add your username to our " +
      s"[users.txt](https://github.com/${org.getLogin}/people/blob/master/users.txt) file " +
      s"_- ideally, a Tech Lead or Dev Manager at ${org.displayName} should open this request for you_."

  override def isSatisfiedBy(user: GHUser)(implicit orgSnapshot: OrgSnapshot) =
    orgSnapshot.sponsoredUserLogins.contains(user.getLogin)

  override def appliesTo(user: GHUser)(implicit orgSnapshot: OrgSnapshot) = true
}

// requires Owner permissions
object TwoFactorAuthRequirement extends AccountRequirement {

  override val issueLabel = "TwoFactorAuth"

  override def fixSummary(implicit org: GHOrganization) =
    "Enable [Two-Factor Authentication](https://help.github.com/articles/about-two-factor-authentication) " +
      "in your [GitHub account settings](https://github.com/settings/admin)."

  override def isSatisfiedBy(user: GHUser)(implicit orgSnapshot: OrgSnapshot) =
    !orgSnapshot.twoFactorAuthDisabledUserLogins.contains(user)

  override def appliesTo(user: GHUser)(implicit orgSnapshot: OrgSnapshot) = !orgSnapshot.botUsers.contains(user)
}
