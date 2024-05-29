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
import com.madgag.github.Implicits._
import com.madgag.scala.collection.decorators.MapDecorator
import com.madgag.scalagithub.GitHub.FR
import com.madgag.scalagithub.commands.CreateComment
import com.madgag.scalagithub.{GitHub, GitHubCredentials, GitHubResponse}
import com.madgag.scalagithub.model.{Issue, Org, Repo, RepoId, Team, User}
import lib.model.{GuWhoOrg, GuWhoOrgUser}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success, Try}

object OrgSnapshot extends Logging {

  private def membersWithout2FA(org: Org)(implicit g: GitHub, mat: Materializer): Future[Set[User]] = {
    org.members.list(Map("filter" -> "2fa_disabled")).all().map(_.toSet) andThen {
      case us => logger.info(s"2fa_disabled count: ${us.map(_.size)}")
    }
  }

  private def fetchAllOrgUsers(org: Org)(implicit g: GitHub, mat: Materializer): Future[Seq[User]] = for {
    allOrgUsers <- org.members.list().all()
    users <- Future.traverse(allOrgUsers)(u => g.getUser(u.login))
  } yield {
    logger.info(s"User count: ${users.size}")
    users.map(_.result)
  }

  private def fetchAllUsersInTeam(org: Org, team_slug: String)(implicit github: GitHub, mat: Materializer): Future[Seq[User]] = for {
    teamOpt <- github.getTeamByName(org.login, team_slug)
    members <- Future.traverse(teamOpt.result.toSeq)(_.members.list().all()).map(_.flatten)
  } yield {
    logger.info(s"'$team_slug' team count: ${members.size}")
    members
  }

  def apply(auditDef: AuditDef)(implicit
    github: GitHub,
    mat: Materializer
  ): Future[OrgSnapshot] = {
    val org: Org = auditDef.guWhoOrg

    val allOrgUsersF = fetchAllOrgUsers(org)
    val usersInAllTeamF = fetchAllUsersInTeam(org, "all")
    val usersInBotsTeamF = fetchAllUsersInTeam(org, "bots")
    val twoFactorAuthDisabledUsersF = membersWithout2FA(org).trying

    for {
      peopleRepoResp <- github.getRepo(org.peopleRepoId)
      peopleRepo = peopleRepoResp.result
      sponsoredUserLogins <- PeopleRepo.getSponsoredUserLogins(peopleRepo)
      allOrgUsers <- allOrgUsersF
      allBotUsers <- usersInBotsTeamF
      twoFactorAuthDisabledUsers <- twoFactorAuthDisabledUsersF
      openIssues <- peopleRepo.issues.list(Map("state" -> "open", "creator" -> auditDef.bot.login)).all()
    } yield OrgSnapshot(
      GuWhoOrg(org, peopleRepo), allOrgUsers.toSet, allBotUsers.toSet, sponsoredUserLogins, twoFactorAuthDisabledUsers, openIssues.toSet
    )
  }
}

case class OrgSnapshot(
  guWhoOrg: GuWhoOrg,
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

  lazy val orgUserProblemsByUser: Map[User, OrgUserProblems] = users.map {
    user =>
      val applicableAvailableEvaluators = availableRequirementEvaluators.filter(_.appliesTo(user)).toSet

      user -> OrgUserProblems(
        applicableRequirements = applicableAvailableEvaluators.map(_.requirement),
        problems = applicableAvailableEvaluators.filterNot(_.isSatisfiedBy(user)).map(_.requirement)
      )(GuWhoOrgUser(guWhoOrg, user))
  }.toMap

  lazy val usersWithProblemsCount = orgUserProblemsByUser.values.count(_.problems.nonEmpty)

  lazy val proportionOfUsersWithProblems = usersWithProblemsCount.toFloat / users.size

  lazy val soManyUsersHaveProblemsThatPerhapsTheGitHubAPIIsBroken = proportionOfUsersWithProblems > 0.9

  lazy val problemUsersExist = usersWithProblemsCount > 0

  lazy val orgUserProblemStats: Map[Int, Int] =
    orgUserProblemsByUser.values.map(_.problems.size).groupBy(identity).mapV(_.size)

  def updateExistingAssignedIssues()(implicit gitHub: GitHub, mat: Materializer) = {
    val issuesWithUserProblems: Set[(Issue, OrgUserProblems)] = for {
      issue <- openIssues
      user <- issue.assignee
      orgUserProblems <- orgUserProblemsByUser.get(user)
    } yield issue -> orgUserProblems

    Future.traverse(issuesWithUserProblems) { case (issue, orgUserProblem) =>
      for {
        labels <- issue.labels.list().all()
      } yield orgUserProblem.updateIssue(IssueWithLabels(issue, labels.toSet))
    }
  }

  def closeUnassignedIssues()(implicit g: GitHub): Future[Set[GitHubResponse[Issue]]] = {
    Future.traverse(openIssues.filter(_.assignee.isEmpty)) {
      _.commentAndClose("Closing this issue as it's not assigned to any user, so this bot can not process it. " +
        "Perhaps the user account was deleted?")
    }
  }

  def createIssuesForNewProblemUsers()(implicit gitHub: GitHub) {
    val usersWithOpenIssues: Set[Long] = openIssues.flatMap(_.assignee).map(_.id)

    val orgUserProblemsList: Set[OrgUserProblems] =
      orgUserProblemsByUser.view.filterKeys(user => usersWithOpenIssues.contains(user.id)).values
        .filter(_.problems.nonEmpty)
        .toSet

    Future.traverse(orgUserProblemsList)(_.createIssue())
  }
}
