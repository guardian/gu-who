/*
 * Copyright 2014 The Guardian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib

import org.kohsuke.github.{GHOrganization, GHUser}
import scala.util.{Success, Try}
import Implicits._

object AccountRequirements {

  val All = Seq(FullNameRequirement, TwoFactorAuthRequirement, SponsorRequirement)

  val RequirementsByLabel = All.map(r => r.issueLabel -> r).toMap

}


trait AccountRequirement {

  trait UserEvaluator {
    val requirement = AccountRequirement.this
    def isSatisfiedBy(user: GHUser): Boolean
    def appliesTo(user: GHUser): Boolean
  }

  val issueLabel: String
  def fixSummary(implicit org: GHOrganization): String
  def userEvaluatorFor(orgSnapshot: OrgSnapshot): Try[UserEvaluator]
}


object FullNameRequirement extends AccountRequirement {
  override val issueLabel = "FullName"
  override def fixSummary(implicit org: GHOrganization) =
    "Enter a full name in your [GitHub profile](https://github.com/settings/profile)."

  def userEvaluatorFor(orgSnapshot: OrgSnapshot) = Success(new UserEvaluator {
    def appliesTo(user: GHUser) = true
    def isSatisfiedBy(user: GHUser) = Option(user.getName()).map(_.length > 1).getOrElse(false)
  })
}

// requires a 'users.txt' file in the people repo
object SponsorRequirement extends AccountRequirement {

  override val issueLabel = "Sponsor"

  override def fixSummary(implicit org: GHOrganization) =
    "Get a pull request opened to add your username to our " +
      s"[users.txt](https://github.com/${org.getLogin}/people/blob/master/users.txt) file " +
      s"_- ideally, a Tech Lead or Dev Manager at ${org.displayName} should open this request for you_."

  def userEvaluatorFor(orgSnapshot: OrgSnapshot) = Success(new UserEvaluator {
    def isSatisfiedBy(user: GHUser) = orgSnapshot.sponsoredUserLogins.contains(user.getLogin)
    def appliesTo(user: GHUser) = true
  })
}

// requires Owner permissions
object TwoFactorAuthRequirement extends AccountRequirement {

  override val issueLabel = "TwoFactorAuth"

  override def fixSummary(implicit org: GHOrganization) =
    "Enable [two-factor authentication](https://help.github.com/articles/about-two-factor-authentication) " +
      "in your [GitHub account settings](https://github.com/settings/admin)."

  def userEvaluatorFor(orgSnapshot: OrgSnapshot) = for (tfaDisabledUsers <- orgSnapshot.twoFactorAuthDisabledUserLogins) yield
    new UserEvaluator {
      def isSatisfiedBy(user: GHUser) = !tfaDisabledUsers.contains(user)
      def appliesTo(user: GHUser) = !orgSnapshot.botUsers.contains(user)
    }
}
