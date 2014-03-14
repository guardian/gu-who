package lib

import org.kohsuke.github._
import collection.convert.wrapAsScala._
import scala.util.{Failure, Success, Try}
import play.api.Logger
import org.joda.time.format.ISODateTimeFormat
import com.github.nscala_time.time.Imports._

object Implicits {
  implicit class RichIssue(issue: GHIssue) {
    lazy val assignee = Option(issue.getAssignee)

    lazy val labelNames = issue.getLabels.map(_.getName)
  }

  implicit class RichOrg(org: GHOrganization) {
    lazy val peopleRepo = org.getRepository("people")

    lazy val allTeam = org.getTeams()("all")

    def testMembership(user: GHUser): Boolean = {
      if (user.isMemberOf(allTeam)) true else {
        val orgMember = user.isMemberOf(org)
        if (orgMember) { allTeam.add(user) }
        orgMember
      }
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

  }
}
