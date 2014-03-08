package controllers

import play.api.mvc._
import org.kohsuke.github._
import collection.convert.wrapAll._
import akka.actor.Actor
import lib.{StateUpdate, AccountRequirement, AccountRequirements, Organisation}
import lib.Implicits._

trait GithubClient {
  val conn = GitHub.connect();
}

object Application extends Controller {

  def index = Action {
    BotScript.run()
    Ok
  }
}

object BotScript {
  def run() = {
    val users = Organisation.testUsers
    val openIssues = Bot.openIssues

    val problemsByUser = users.map(u => u -> AccountRequirements.failedBy(u)).toMap.withDefaultValue(Set.empty)

    createIssuesForNewProblemUsers(problemsByUser, openIssues)

    updateExisting(openIssues, problemsByUser)
  }

  def updateExisting(openIssues: Set[GHIssue], problemsByUser: Map[GHUser, Set[AccountRequirement]]) {
    for {
      issue <- openIssues
      user <- issue.assignee
    } Issue.update(issue, problemsByUser(user))
  }

  def createIssuesForNewProblemUsers(problemsByUser: Map[GHUser, Set[AccountRequirement]], openIssues: Set[GHIssue]) {
    val usersWithOpenIssues = openIssues.flatMap(_.assignee)
    for {
      (user, problems) <- problemsByUser -- usersWithOpenIssues
      if problems.nonEmpty
    } Issue.create(user, problems)
  }
}

object Bot extends GithubClient {
  val bot = conn.getMyself
  lazy val openIssues = Organisation.peopleRepo.getIssues(GHIssueState.OPEN).toSet.filter(_.getUser==bot)
}

object Issue {

  def create(user: GHUser, problems: Set[AccountRequirement]) {
    require(problems.nonEmpty)

    Organisation.checkAllTeamMembership(user)

    val title = s"@${user.getLogin}: The Guardian asks you to fix your GitHub account!"
    val description = views.html.issue(user, problems).body

    val issue = Organisation.peopleRepo.createIssue(title)
    for (p <- problems) { issue.label(p.issueLabel) }
    issue.assignee(user).body(description).create()
  }

  def update(issue: GHIssue, currentProblems: Set[AccountRequirement]) {
    val stateUpdate = StateUpdate(issue, currentProblems)

    if (stateUpdate.isChange) {
      val unassociatedLabels = issue.getLabels.map(_.getName).filterNot(AccountRequirements.AllLabels)
      val newLabelSet = currentProblems.map(_.issueLabel) ++ unassociatedLabels
      issue.setLabels(newLabelSet.toSeq: _*)
    }

    if (stateUpdate.worthyOfComment) {
      issue.comment(views.html.update(stateUpdate).body)
    }

    if (stateUpdate.issueCanBeClosed) {
      issue.close()
    }
  }
}

class GitHubPoller extends Actor {
  def receive = {
    case RunScript => BotScript.run()
  }
}

case object AddTeamMembers

case object RunScript

