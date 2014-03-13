package lib

import collection.convert.wrapAsScala._
import scalax.file.Path
import com.squareup.okhttp.{OkHttpClient, HttpResponseCache}
import scalax.file.ImplicitConversions._
import org.kohsuke.github.GitHub
import java.net.URL

object AuditDef {
  def safelyCreateFor(orgName: String, apiKey: String): AuditDef = {
    val org = GitHub.connectUsingOAuth(apiKey).getOrganization(orgName)
    AuditDef(org.getLogin, apiKey: String)
  }
}

case class AuditDef(orgLogin: String, apiKey: String) {

  val workingDir = Path.fromString("working-dir") / orgLogin.toLowerCase

  val httpResponseCache = new HttpResponseCache(workingDir / "http-cache", 5 * 1024 * 1024)

  lazy val okHttpClient = {
    val client = new OkHttpClient
    client.setOkResponseCache(httpResponseCache)
    client
  }

  def conn() = new GitHub(null, apiKey, null) {
    override def open(url: URL) = okHttpClient.open(url)
  }

  lazy val (org, bot) = {
    val c = conn()

    val org = c.getOrganization(orgLogin)

    require(c.getMyOrganizations.values.map(_.getId).toSet.contains(org.getId))

    (org, c.getMyself)
  }


}
