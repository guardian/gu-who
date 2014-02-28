package lib

import org.kohsuke.github.GHUser
import controllers.GithubClient

object Organisation extends GithubClient {

  val guardian = conn.getOrganization("guardian")
  val guardianMembers = guardian.getMembers
  lazy val peopleRepo = conn.getRepository("guardian/people")
  val testUsers = List(conn.getUser("lindseydew"), conn.getUser("rtyley"))

  lazy val allTeam = guardian.getTeams.get("all").getMembers

  def getUser(user: GHUser): GHUser = conn.getUser(user.getLogin)

  def checkAllTeamMembership(user: GHUser) = {
    if(!isMemberOfAllTeam(user)) addToAllTeam(user)
  }

  //add to the proper github repo not your list representation...!
  def addToAllTeam(user: GHUser) =  allTeam.add(user)

  def isMemberOfAllTeam(user: GHUser): Boolean = { allTeam.contains(user) }

  lazy val twoFactorAuthDisabledUserLogins = guardian.getMembersWithFilter("2fa_disabled").asList()
}
