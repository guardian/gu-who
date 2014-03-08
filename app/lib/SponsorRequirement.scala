package lib

import org.kohsuke.github.GHUser

object SponsorRequirement extends AccountRequirement {

  val sponsoredUserLogins = Set("lindseydew")

  override val issueLabel = "Sponsor"

  override val fixSummary =
    "Get a Pull-Request opened to add your username to our " +
      s"[users.txt](https://github.com/${Organisation.org.getLogin}/people/blob/master/users.txt) file " +
      s"_- ideally, a Tech Lead or Dev Manager at ${Organisation.org.getName} should open this request for you_."

  override def isSatisfiedBy(user: GHUser): Boolean = sponsoredUserLogins.contains(user.getLogin)

}
