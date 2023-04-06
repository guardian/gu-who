/*
 * Copyright 2023 The Guardian
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

package com.gu.who

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.gu.who.logging.Logging
import lib._

import scala.concurrent.ExecutionContext.Implicits.global

object Lambda extends Logging {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = Materializer(actorSystem)

  def scan(auditDef: AuditDef) =  {
    new Dogpile[OrgSnapshot](
      for (orgSnapshot <- OrgSnapshot(auditDef)) yield {
        logger.info(s"availableRequirementEvaluators=${orgSnapshot.availableRequirementEvaluators} ${orgSnapshot.orgUserProblemStats}")

        if (orgSnapshot.soManyUsersHaveProblemsThatPerhapsTheGitHubAPIIsBroken) {
          logger.error(s"usersWithProblemsCount=${orgSnapshot.usersWithProblemsCount} - it's possible the GitHub API is broken, so no action will be taken, to avoid spamming users")
        } else {
          orgSnapshot.createIssuesForNewProblemUsers()

          orgSnapshot.updateExistingAssignedIssues()

          orgSnapshot.closeUnassignedIssues()
        }
        orgSnapshot
      }
    )
  }.doAtLeastOneMore()
}
