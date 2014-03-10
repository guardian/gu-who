package lib

import org.kohsuke.github.{GitHub, GHOrganization}
import collection.convert.wrapAsScala._

case class AuditDef(githubApiKey: String, org: GHOrganization) {

  def conn() = GitHub.connectUsingOAuth(githubApiKey)

  lazy val bot = conn().getMyself

  require(conn().getMyOrganizations.values.map(_.getId).toSet.contains(org.getId))
}
