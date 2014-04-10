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

import org.kohsuke.github._
import collection.convert.wrapAsScala._
import scala.util.{Success, Try}
import com.github.nscala_time.time.Imports._
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.kohsuke.github.GHOrganization.Permission

object Implicits {
  implicit class RichFuture[S](f: Future[S]) {
    lazy val trying = {
      val p = Promise[Try[S]]()
      f.onComplete { case t => p.complete(Success(t)) }
      p.future
    }
  }

  implicit class RichIssue(issue: GHIssue) {
    lazy val assignee = Option(issue.getAssignee)

    lazy val labelNames = issue.getLabels.map(_.getName)
  }

  implicit class RichOrg(org: GHOrganization) {

    lazy val peopleRepo = org.getRepository("people")

    lazy val teamsByName: Map[String, GHTeam] = org.getTeams().toMap

    lazy val allTeamOpt = teamsByName.get("all")

    lazy val allTeam =  allTeamOpt.getOrElse(createAllTeam)

    lazy val botsTeamOpt = teamsByName.get("bots")

    def testMembership(user: GHUser): Boolean = {
      if (user.isMemberOf(allTeam)) true else {
        val orgMember = user.isMemberOf(org)
        if (orgMember) { allTeam.add(user) }
        orgMember
      }
    }

    def createAllTeam: GHTeam = {
      val team = org.createTeam("all", Permission.PULL)
      team.add(peopleRepo)
      team
    }
  }

  implicit class RichTeam(team: GHTeam) {
    def ensureHasMember(user: GHUser) {
      if (!team.getMembers.contains(user)) {
        team.add(user)
      }
    }
  }

  val dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")

  implicit class RichPerson(person: GHPerson) {

    lazy val createdAt = dateTimeFormatter.parseDateTime(person.getCreatedAt)

    lazy val atLogin = s"@${person.getLogin}"

    lazy val displayName = Option(person.getName).filter(_.nonEmpty).getOrElse(atLogin)

  }
}
