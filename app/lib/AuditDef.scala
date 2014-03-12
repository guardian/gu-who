package lib

import org.kohsuke.github.GHOrganization
import collection.convert.wrapAsScala._

case class AuditDef(githubApiKey: String, org: GHOrganization) {

  def conn() = OkGitHub.conn(githubApiKey)

  lazy val bot = {
    val c = conn()

    require(c.getMyOrganizations.values.map(_.getId).toSet.contains(org.getId))

    c.getMyself
  }

}
