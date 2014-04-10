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

import collection.convert.wrapAsScala._
import scalax.file.Path
import com.squareup.okhttp.{OkHttpClient, HttpResponseCache}
import scalax.file.ImplicitConversions._
import org.kohsuke.github.GitHub
import java.net.URL
import Implicits._
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import play.api.Logger

object AuditDef {
  val parentWorkDir = Path.fromString("/tmp") / "working-dir"

  def safelyCreateFor(orgName: String, apiKey: String): AuditDef = {
    val org = GitHub.connectUsingOAuth(apiKey).getOrganization(orgName)
    AuditDef(org.getLogin, apiKey: String)
  }
}

case class AuditDef(orgLogin: String, apiKey: String) {


  val workingDir = AuditDef.parentWorkDir / orgLogin.toLowerCase

  workingDir.mkdirs()

  lazy val okHttpClient = {
    val client = new OkHttpClient

    val responseCacheDir = workingDir / "http-cache"
    responseCacheDir.mkdirs()
    if (responseCacheDir.exists) {
      val httpResponseCache = new HttpResponseCache(responseCacheDir, 5 * 1024 * 1024)
      client.setOkResponseCache(httpResponseCache)
    } else {
      Logger.warn(s"Couldn't create HttpResponseCache dir ${responseCacheDir.path}")
    }
    client
  }

  def conn() = new GitHub(null, apiKey, null) {
    override def open(url: URL) = okHttpClient.open(url)
  }

  lazy val (org, bot) = {
    val c = conn()

    val org = c.getOrganization(orgLogin)

    val bot = c.getMyself

    require(bot.isMemberOf(org))

    (org, bot)
  }

  lazy val seemsLegit = bot.isMemberOf(org) && org.listPublicMembers.exists(_.createdAt < DateTime.now - 3.months)

}
