package controllers

import play.api._
import play.api.mvc._
import org.kohsuke.github.{GHUser, GHOrganization, GitHub}
import collection.convert.decorateAll._
object Application extends Controller {

  def index = Action {
    import Github._
    for{m<-testUsers
        if(!isMemberOfAllTeam(m))
    } { addToAllTeam(m) }
    Ok(views.html.index("Your new application is ready."))
  }



}

object Github {
  val github = GitHub.connect();

  val testUsers = List(github.getMyself, github.getUser("rtyley"))
  val guardian = github.getOrganization("guardian")
  val guardianMembers = guardian.getMembers
  val allTeam = guardian.getTeams.get("all")

  def addToAllTeam(user: GHUser) = {
    println("adding user to member")
    allTeam.add(user)
  }


  def isMemberOfAllTeam(user: GHUser): Boolean = {
    allTeam.getMembers.contains(user)
  }
}