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

import com.madgag.scalagithub.model.{Org, User}
import lib.StateUpdate.markdownFor

import java.time.Instant

sealed trait StateUpdate {
  val issueCanBeClosed: Boolean
}

object StateUpdate {
  def markdownFor(problems: Set[AccountRequirement])(implicit org: Org): String =
    problems.map(p => s"* ${p.fixSummary}").mkString("\n")
}

case object UserHasLeftOrg extends StateUpdate {
  override val issueCanBeClosed = true
}

case class MemberUserUpdate(oldProblems: Set[AccountRequirement],
                            currentProblems: Set[AccountRequirement],
                            terminationDate: Instant,
                            orgMembershipWillBeConcealed: Boolean,
                            terminationWarning: Option[TerminationSchedule]) extends StateUpdate {

  val isChange = oldProblems != currentProblems

  val issueCanBeClosed = currentProblems.isEmpty

  val userShouldReceiveFinalWarning = terminationWarning.isDefined

  val worthyOfComment = issueCanBeClosed || isChange || orgMembershipWillBeConcealed || userShouldReceiveFinalWarning

  val fixedRequirements = oldProblems -- currentProblems

  def asMarkdown()(implicit org: Org): String = {
    (Option.when(userShouldReceiveFinalWarning) {
      "**WARNING:** If requirements for this account aren't met, it will be removed from " +
        s"${org.displayName}'s organisation on ${terminationDate}."
    } ++ Option.when(fixedRequirements.nonEmpty) {
      s"Thanks for fixing those requirements (ie ${fixedRequirements.map(_.issueLabel).mkString(", ")})."
    } ++ (if (issueCanBeClosed) Seq("Closing this issue, you're good to go! :sparkles:") else {
      Seq(s"""These are the remaining requirements you need to address:
         |
         |${markdownFor(currentProblems)}
         |
         |""".stripMargin) ++ Option.when(orgMembershipWillBeConcealed)(
        s"""We've made your membership of ${org.displayName}'s organisation on GitHub non-public, to make it less likely an attacker can find it.
           |
           |Once you've addressed the requirements above, you (and [only](https://help.github.com/articles/publicizing-or-hiding-organization-membership) you) can [re-publicise your membership](${org.membersAdminUrl}) if it's appropriate.
           |""".stripMargin
      )
    })).mkString("\n\n")
  }
}

case class MembershipTermination(problems: Set[AccountRequirement]) extends StateUpdate {
  override val issueCanBeClosed = true

  def asMarkdown()(implicit user: User, org: Org): String = {
    s"""Removing ${user.atLogin} from organisation. These were the outstanding requirements:
       |
       |${markdownFor(problems)}"""
  }
}