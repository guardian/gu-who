package lib

import org.kohsuke.github.GHUser

object TwoFactorAuthRequirement extends AccountRequirement {

  override val issueLabel = "TwoFactorAuth"

  override val fixSummary =
    "Enable [Two-Factor Authentication](https://help.github.com/articles/about-two-factor-authentication) " +
      "in your [GitHub account settings](https://github.com/settings/admin)."

  override def isSatisfiedBy(user: GHUser)(implicit orgSnapshot: OrgSnapshot) =
    !orgSnapshot.twoFactorAuthDisabledUserLogins.contains(user)

}
