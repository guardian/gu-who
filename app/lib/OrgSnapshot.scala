package lib

import org.kohsuke.github.{GHOrganization, GHIssueState, GHIssue, GHUser}
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import collection.convert.wrapAsScala._
import concurrent._
import ExecutionContext.Implicits.global
import scalax.file._
import scalax.file.ImplicitConversions._
import play.api.Logger
import Implicits._

object OrgSnapshot {
  val testUserLogins = Set("rtyley", "lindseydew")
  
  def apply(auditDef: AuditDef): Future[OrgSnapshot] = {
    val org = auditDef.org
    val peopleRepo = org.peopleRepo

    val usersF = future {
      org.getMembers.toSet.filter(u => testUserLogins(u.getLogin)).map(u => auditDef.conn().getUser(u.getLogin))
    } flatMap {
      Future.traverse(_)(u => future { auditDef.conn().getUser(u.getLogin) })
    } andThen { case us => Logger.info(s"User count: ${us.map(_.size)}") }

    val sponsoredUserLoginsF = future {
      PeopleRepo.getSponsoredUserLogins(
        Path("sponsors") / org.getLogin,
        peopleRepo.gitHttpTransportUrl,
        Some(new UsernamePasswordCredentialsProvider(auditDef.githubApiKey, ""))
      )
    }

    val twoFactorAuthDisabledUsersF = future {
      org.getMembersWithFilter("2fa_disabled").asList().toSet
    } andThen { case us => Logger.info(s"2fa_disabled count: ${us.map(_.size)}") }

    val openIssuesF = future {
      peopleRepo.getIssues(GHIssueState.OPEN).toSet.filter(_.getUser==auditDef.bot)
    } andThen { case is => Logger.info(s"Open issue count: ${is.map(_.size)}") }

    for {
      users <- usersF
      sponsoredUserLogins <- sponsoredUserLoginsF
      twoFactorAuthDisabledUsers <- twoFactorAuthDisabledUsersF
      openIssues <- openIssuesF
    } yield OrgSnapshot(org, users, sponsoredUserLogins, twoFactorAuthDisabledUsers, openIssues)
  }
}

case class OrgSnapshot(
  org: GHOrganization,
  users: Set[GHUser],
  sponsoredUserLogins: Set[String],
  twoFactorAuthDisabledUserLogins: Set[GHUser],
  openIssues: Set[GHIssue]
) {

  lazy val problemsByUser = users.map(u => u -> AccountRequirements.failedBy(u)(this)).toMap.withDefaultValue(Set.empty)

  def updateExistingIssues() {
    for {
      issue <- openIssues
      user <- issue.assignee
    } OrgUserProblems(org, user, problemsByUser(user)).updateIssue(issue)
  }

  def createIssuesForNewProblemUsers() {
    val usersWithOpenIssues = openIssues.flatMap(_.assignee)
    for {
      (user, problems) <- problemsByUser -- usersWithOpenIssues
      if problems.nonEmpty
    } OrgUserProblems(org, user, problems).createIssue()
  }
}
