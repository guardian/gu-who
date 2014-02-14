package controllers

import play.api._
import play.api.mvc._
import org.kohsuke.github.{GHUser, GitHub}
import collection.convert.wrapAll._
import akka.actor.Actor

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
}

object Github {
  val github = GitHub.connect();

  val testUsers = List(github.getUser("lindseydew"), github.getUser("rtyley"))
  val guardian = github.getOrganization("guardian")
  val guardianMembers = guardian.getMembers

  lazy val allTeam = guardian.getTeams.get("all").getMembers

  def addToAllTeam(user: GHUser) = {
    allTeam.add(user)
  }
  def isMemberOfAllTeam(user: GHUser): Boolean = {
    allTeam.contains(user)
  }
}

class GitHubPoller extends Actor {
  def receive = {
    case AddTeamMembers => {
      import Github._
      for { m <- guardianMembers if (!isMemberOfAllTeam(m)) } {
        Logger.info(s"adding $m to all team")
        addToAllTeam(m)
      }
    }
  }
}

case object AddTeamMembers