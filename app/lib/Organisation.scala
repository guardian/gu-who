package lib

import org.kohsuke.github.GHUser
import controllers.GithubClient
import collection.convert.wrapAsScala._

object Organisation extends GithubClient {

  val org = {
    val orgs = conn.getMyOrganizations.values
    require(orgs.size == 1, "The bot should have membership of exactly one org.")
    orgs.head
  }
  
  val orgMembers = org.getMembers
  lazy val peopleRepo = org.getRepository("people")
  val testUsers = Seq(conn.getUser("lindseydew"), conn.getUser("rtyley"))

  lazy val allTeam = org.getTeams.get("all").getMembers

  def getUser(user: GHUser): GHUser = conn.getUser(user.getLogin)

  def checkAllTeamMembership(user: GHUser) = {
    if(!isMemberOfAllTeam(user)) addToAllTeam(user)
  }

  //add to the proper github repo not your list representation...!
  def addToAllTeam(user: GHUser) =  allTeam.add(user)

  def isMemberOfAllTeam(user: GHUser): Boolean = { allTeam.contains(user) }

  lazy val twoFactorAuthDisabledUserLogins = org.getMembersWithFilter("2fa_disabled").asList().toSet
}
