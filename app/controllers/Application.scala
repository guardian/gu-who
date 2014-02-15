package controllers

import play.api._
import play.api.mvc._
import org.kohsuke.github._
import collection.convert.wrapAll._
import akka.actor.Actor

trait GithubClient {
  val conn = GitHub.connect();
}

object Application extends Controller {

  def index = Action {

    BotScript.run()

    Ok(views.html.index("Your new application is ready."))
  }
}


object BotScript {
  def run() = {

    val users = GuardianUsers.testUsers
    val openIssues = Bot.openIssues

    val openIssuesByUser = (for(i<-openIssues) yield i.getAssignee -> i).toMap

    val problemsByUser = (for {
        u <- users;
        problems = AccountRequirements.failedBy(u);
        if(!problems.isEmpty)
    } yield u -> problems).toMap

    val usersWithIssuesToCreate = problemsByUser.keySet.diff(openIssuesByUser.keySet)

    for(user <- usersWithIssuesToCreate) {
      GuardianUsers.checkAllTeamMembership(user)
      Issue.create(user, problemsByUser(user))
    }

    for(i<-openIssues; user = i.getAssignee) {
      Issue.update(i, problemsByUser(user))
    }

  }
}

object Bot extends GithubClient {
  val bot = conn.getUser("lindseydew")
  lazy val openIssues = GuardianUsers.peopleRepo.getIssues(GHIssueState.OPEN).toList
}

object Issue {

  def create(user: GHUser, problems: Set[AccountRequirement]): Unit = {
    println(s"creating issue ${user} and problems ${problems}")
  }

  def update(issue: GHIssue, problems: Set[AccountRequirement]): Unit = {
    println(s"updating issue ${issue} and problems ${problems}")
  }

}

object GuardianUsers extends GithubClient {

  val guardian = conn.getOrganization("guardian")
  val guardianMembers = guardian.getMembers
  lazy val peopleRepo = conn.getRepository("guardian/people")
  val testUsers = List(conn.getUser("lindseydew"), conn.getUser("rtyley"))

  lazy val allTeam = guardian.getTeams.get("all").getMembers

  def getUser(user: GHUser): GHUser = conn.getUser(user.getLogin)

  def checkAllTeamMembership(user: GHUser) = {
    if(GuardianUsers.isMemberOfAllTeam(user)) addToAllTeam(user)
  }

  def addToAllTeam(user: GHUser) =  allTeam.add(user)

  def isMemberOfAllTeam(user: GHUser): Boolean = { allTeam.contains(user) }

  lazy val twoFactorAuthDisabledUserLogins = guardian.getMembersWithFilter("2fa_disabled").asList()
}

trait AccountRequirement {

  val issueLabel: String
  val fixSummary: String
  def isSatisfiedBy(user: GHUser): Boolean
}

object FullNameRequirement extends AccountRequirement {
  override val issueLabel = "FullName"
  override val fixSummary = "Enter a full name in your [GitHub profile](https://github.com/settings/profile)."

  def isSatisfiedBy(user: GHUser) = Option(user.getName()).map(_.length > 6).getOrElse(false)

}

object TwoFactorAuthRequirement extends AccountRequirement {

  override val issueLabel = "TwoFactorAuth"

  override val fixSummary =
    "Enable [Two-Factor Authentication](https://help.github.com/articles/about-two-factor-authentication) " +
      "in your [GitHub account settings](https://github.com/settings/admin)."

  override def isSatisfiedBy(user: GHUser) = !GuardianUsers.twoFactorAuthDisabledUserLogins.contains(user.getLogin)

}

object SponsorRequirement extends AccountRequirement {

  val sponsoredUserLogins = Set("lindseydew")

  override val issueLabel = "Sponsor"

  override val fixSummary =
    "Get a Pull-Request opened to add your username to our " +
      "[users.txt](https://github.com/guardian/people/blob/master/users.txt) file " +
      "_- ideally, a Guardian Tech Lead or Dev Manager should open this request for you_."

  override def isSatisfiedBy(user: GHUser): Boolean = sponsoredUserLogins.contains(user.getLogin)

}

object AccountRequirements {
  val All = Seq(FullNameRequirement, TwoFactorAuthRequirement, SponsorRequirement)

  def failedBy(user: GHUser): Set[AccountRequirement] = All.filterNot(_.isSatisfiedBy(user)).toSet
}

class GitHubPoller extends Actor {
  def receive = {
    case RunScript => BotScript.run()
  }
}

case object AddTeamMembers

case object RunScript

