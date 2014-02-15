package lib

import org.kohsuke.github.GHUser

object FullNameRequirement extends AccountRequirement {
  override val issueLabel = "FullName"
  override val fixSummary = "Enter a full name in your [GitHub profile](https://github.com/settings/profile)."

  def isSatisfiedBy(user: GHUser) = Option(user.getName()).map(_.length > 6).getOrElse(false)

}
