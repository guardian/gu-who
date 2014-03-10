package lib

import org.kohsuke.github.{GHUser, GHTeam, GHOrganization, GHIssue}
import collection.convert.wrapAsScala._
import scala.util.{Failure, Success, Try}
import play.api.Logger

object Implicits {
  implicit class RichIssue(issue: GHIssue) {
    lazy val assignee = Option(issue.getAssignee)

    lazy val labelNames = issue.getLabels.map(_.getName)
  }

  implicit class RichOrg(org: GHOrganization) {
    lazy val peopleRepo = org.getRepository("people")

    lazy val allTeam = org.getTeams()("all")

    def testMembership(user: GHUser): Boolean = {
      val isMemberOfOrg = user.isMemberOf(org)
      if (isMemberOfOrg) { allTeam.add(user) }
      isMemberOfOrg
    }
  }

  implicit class RichTeam(team: GHTeam) {
    def ensureHasMember(user: GHUser) {
      if (!team.getMembers.contains(user)) {
        team.add(user)
      }
    }
  }
}
