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

import com.madgag.scalagithub.model._
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