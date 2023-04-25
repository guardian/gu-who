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

import com.github.nscala_time.time.Imports._
import com.madgag.scalagithub.GitHub
import com.madgag.scalagithub.GitHub.FR
import com.madgag.scalagithub.model.{Issue, Org, RepoId}
import org.kohsuke.github._
import play.api.Logging

import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent._
import scala.util.{Success, Try}

object Implicits {
  implicit class RichFuture[S](f: Future[S]) {
    def trying(implicit ec: ExecutionContext): Future[Try[S]] = {
      val p = Promise[Try[S]]()
      f.onComplete { case t => p.complete(Success(t)) }
      p.future
    }
  }

  implicit class RichIssue(issue: Issue) {
//    lazy val assignee = Option(issue.getAssignee)
//
//    lazy val labelNames = issue.getLabels.asScala.map(_.getName)

    def commentAndClose(text: String)(implicit g: GitHub, ec: ExecutionContext): FR[Issue] = for {
      _ <- issue.createComment(text)
      issue <- issue.close()
    } yield issue
  }

  implicit class RichOrg(org: Org) extends Logging {

    val peopleRepoId: RepoId = RepoId(org.login, "people")

    // lazy val allTeamOpt = teamsByName.get("all")

//    lazy val allTeam =  {
//      val team = allTeamOpt.getOrElse(createAllTeam)
//      logger.info(s"'${team.getName}' team : permission=${team.getPermission} people-repo-access=${team.getRepositories.asScala.contains("people")}")
//      team
//    }

//    lazy val botsTeamOpt = teamsByName.get("bots")

//    def testMembership(user: User): Boolean = {
//      if (user.isMemberOf(allTeam)) true else {
//        val orgMember = user.isMemberOf(org)
//        logger.info(s"user ${user.atLogin} NOT in 'all' team. orgMember=${orgMember}")
//        if (orgMember) { allTeam.add(user) }
//        orgMember
//      }
//    }

//    def createAllTeam: GHTeam = {
//      logger.info(s"Creating 'all' team for ${org.atLogin}")
//      val team = org.createTeam("all", Permission.PULL)
//      team.add(peopleRepo)
//      team
//    }
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

    lazy val createdAt = new DateTime(person.getCreatedAt)

    lazy val atLogin = s"@${person.getLogin}"

    lazy val displayName = Option(person.getName).filter(_.nonEmpty).getOrElse(atLogin)

  }

  implicit class RichZonedDateTime(zdt: ZonedDateTime) {
    val asLegacyDateTime: DateTime =
      new DateTime(zdt.toInstant.asLegacyJodaInstant, zdt.getZone.asLegacyDateTimeZone)
  }

  implicit class RichDateTime(dt: org.joda.time.DateTime) {
    val asInstant: java.time.Instant = java.time.Instant.ofEpochMilli(dt.getMillis)
  }

  implicit class RichDateTimeZone(legacyDTZ: DateTimeZone) {
    val asZoneId: ZoneId = ZoneId.of(legacyDTZ.getID)
  }

  implicit class RichZoneId(zoneId: ZoneId) {
    val asLegacyDateTimeZone: DateTimeZone = DateTimeZone.forID(zoneId.getId)
  }

  implicit class RichInstant(instant: java.time.Instant) {
    val asLegacyJodaInstant: org.joda.time.Instant = org.joda.time.Instant.ofEpochMilli(instant.toEpochMilli)
  }
}
