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
    lazy val displayName = Option(org.getName).getOrElse(s"@${org.getLogin}")

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

  }
}
