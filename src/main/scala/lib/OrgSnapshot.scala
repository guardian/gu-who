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

import akka.stream.Materializer
import com.gu.who.logging.Logging
import com.madgag.scalagithub.GitHub.FR
import com.madgag.scalagithub.commands.CreateComment
import com.madgag.scalagithub.{GitHub, GitHubCredentials}
import com.madgag.scalagithub.model.{Issue, Org, Repo, Team, User}
import lib.Implicits._
import lib.gitgithub.RichSource
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success, Try}

object OrgSnapshot extends Logging {
  
  def apply(auditDef: AuditDef)(implicit
    github: GitHub,
    mat: Materializer
  ): Future[OrgSnapshot] = {
    val org: Org = ???
    val peopleRepo: com.madgag.scalagithub.model.Repo = ???

    // val org = Await.result(github.getOrg(orgName), 4.seconds)

    import lib.gitgithub

    val allOrgUsersF: Future[Seq[User]] = org.members.list().all()
    val usersF = allOrgUsersF flatMap {
      Future.traverse(_)(u => github.getUser(u.login))
    } andThen { case us => logger.info(s"User count: ${us.map(_.size)}") }

    val sponsoredUserLoginsF: Future[Set[String]] = PeopleRepo.getSponsoredUserLogins(peopleRepo)

    val botUsersF: Future[Set[User]] = Future {
      org.botsTeamOpt.toSeq.flatMap(_.getMembers.toSeq).toSet
    } andThen { case us => Logger.info(s"bots team count: ${us.map(_.size)}") }

    val twoFactorAuthDisabledUsersF =
      org.members.list(Map("filter" -> "2fa_disabled")).all().map(_.toSet) andThen {
        case us => logger.info(s"2fa_disabled count: ${us.map(_.size)}")
      }

    val openIssuesF = Future {
      peopleRepo.getIssues(GHIssueState.OPEN).toSet.filter(_.getUser==auditDef.bot)
    } andThen { case is => Logger.info(s"Open issue count: ${is.map(_.size)}") }

    for {
      users <- usersF
      sponsoredUserLogins <- sponsoredUserLoginsF
      twoFactorAuthDisabledUsers <- twoFactorAuthDisabledUsersF.trying
      openIssues <- openIssuesF
      botUsers <- botUsersF
    } yield OrgSnapshot(org, users, botUsers, sponsoredUserLogins, twoFactorAuthDisabledUsers, openIssues)
  }
}

case class OrgSnapshot(
  org: Org,
  peopleRepo: Repo,
  allTeam: Team,
  botsTeam: Team,
  users: Set[User],
  botUsers: Set[User],
  sponsoredUserLogins: Set[String],
  twoFactorAuthDisabledUserLogins: Try[Set[User]],
  openIssues: Set[Issue]
) {

  lazy val sponsoredUserLoginsLowerCase: Set[String] = sponsoredUserLogins.map(_.toLowerCase)

  def hasSponsorFor(user: User) = sponsoredUserLoginsLowerCase.contains(user.login.toLowerCase)

  private lazy val evaluatorsByRequirement =
    AccountRequirements.All.map(ar => ar -> ar.userEvaluatorFor(this)).toMap

  lazy val availableRequirementEvaluators: Iterable[AccountRequirement#UserEvaluator] =
    evaluatorsByRequirement.collect { case (_, Success(evaluator)) => evaluator }

  lazy val requirementsWithUnavailableEvaluators = evaluatorsByRequirement.collect { case (ar, Failure(t)) => ar -> t }

  lazy val orgUserProblemsByUser = users.map {
    user =>
      val applicableAvailableEvaluators = availableRequirementEvaluators.filter(_.appliesTo(user)).toSet

      user -> OrgUserProblems(
        org,
        user,
        applicableRequirements = applicableAvailableEvaluators.map(_.requirement),
        problems = applicableAvailableEvaluators.filterNot(_.isSatisfiedBy(user)).map(_.requirement)
      )
  }.toMap

  lazy val usersWithProblemsCount = orgUserProblemsByUser.values.count(_.problems.nonEmpty)

  lazy val proportionOfUsersWithProblems = usersWithProblemsCount.toFloat / users.size

  lazy val soManyUsersHaveProblemsThatPerhapsTheGitHubAPIIsBroken = proportionOfUsersWithProblems > 0.9

  lazy val problemUsersExist = usersWithProblemsCount > 0

  lazy val orgUserProblemStats = orgUserProblemsByUser.values.map(_.problems.size).groupBy(identity).mapValues(_.size)

  def updateExistingAssignedIssues() {
    for {
      issue <- openIssues
      user <- issue.assignee
      orgUserProblems <- orgUserProblemsByUser.get(user)
    } orgUserProblems.updateIssue(issue)
  }

  def closeUnassignedIssues()(implicit gitHub: GitHub): Future[Unit] = {
    for {
      issue: Issue <- openIssues if issue.assignee.isEmpty
    } {
      issue.comments2.create(CreateComment(
        "Closing this issue as it's not assigned to any user, so this bot can not process it. " +
          "Perhaps the user account was deleted?"
      ))

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
