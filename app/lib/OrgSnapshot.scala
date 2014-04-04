package lib

import org.kohsuke.github._
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import collection.convert.wrapAsScala._
import concurrent._
import ExecutionContext.Implicits.global
import scalax.file._
import scalax.file.ImplicitConversions._
import play.api.Logger
import Implicits._

object OrgSnapshot {
  
  def apply(auditDef: AuditDef): Future[OrgSnapshot] = {
    val org = auditDef.org
    val peopleRepo = org.peopleRepo
    val conn = auditDef.conn()

    val usersF = future {
      org.listMembers.map { u => conn.getUser(u.getLogin) }.toSet
    } flatMap {
      Future.traverse(_)(u => future { conn.getUser(u.getLogin) })
    } andThen { case us => Logger.info(s"User count: ${us.map(_.size)}") }

    val sponsoredUserLoginsF = future {
      PeopleRepo.getSponsoredUserLogins(
        auditDef.workingDir,
        peopleRepo.gitHttpTransportUrl,
        Some(new UsernamePasswordCredentialsProvider(auditDef.apiKey, ""))
      )
    }

    val botUsersF = future {
      org.botsTeam.getMembers.toSet
    } andThen { case us => Logger.info(s"bots team count: ${us.map(_.size)}") }

    val twoFactorAuthDisabledUsersF = future {
      org.listMembersWithFilter("2fa_disabled").asList().toSet
    } andThen { case us => Logger.info(s"2fa_disabled count: ${us.map(_.size)}") }  

    val openIssuesF = future {
      peopleRepo.getIssues(GHIssueState.OPEN).toSet.filter(_.getUser==auditDef.bot)
    } andThen { case is => Logger.info(s"Open issue count: ${is.map(_.size)}") }

    for {
      users <- usersF
      sponsoredUserLogins <- sponsoredUserLoginsF
      twoFactorAuthDisabledUsers <- twoFactorAuthDisabledUsersF
      openIssues <- openIssuesF
      botUsers <- botUsersF
    } yield OrgSnapshot(org, users, botUsers, sponsoredUserLogins, twoFactorAuthDisabledUsers, openIssues)
  }
}

case class OrgSnapshot(
  org: GHOrganization,
  users: Set[GHUser],
  botUsers: Set[GHUser],
  sponsoredUserLogins: Set[String],
  twoFactorAuthDisabledUserLogins: Set[GHUser],
  openIssues: Set[GHIssue]
) {

  lazy val orgUserProblemsByUser = users.map {
    user =>
      val applicableRequirements = AccountRequirements.applicableTo(user)(this)
      val failedRequirements = applicableRequirements.filterNot(_.isSatisfiedBy(user)(this))
      user -> OrgUserProblems(org, user, applicableRequirements, failedRequirements)
  }.toMap

  def updateExistingAssignedIssues() {
    for {
      issue <- openIssues
      user <- issue.assignee
      orgUserProblems <- orgUserProblemsByUser.get(user)
    } orgUserProblems.updateIssue(issue)
  }

  def closeUnassignedIssues() {
    for {
      issue <- openIssues if issue.assignee.isEmpty
    } {
      issue.comment(
        "Closing this issue as it's not assigned to any user, so this bot can not process it. " +
          "Perhaps the user account was deleted?")
      issue.close()
    }
  }

  def createIssuesForNewProblemUsers() {
    val usersWithOpenIssues = openIssues.flatMap(_.assignee)
    for {
      (user, orgUserProblems) <- orgUserProblemsByUser -- usersWithOpenIssues
      if orgUserProblems.problems.nonEmpty
    } orgUserProblems.createIssue()
  }
}
