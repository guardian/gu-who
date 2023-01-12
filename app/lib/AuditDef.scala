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

import com.github.nscala_time.time.Imports._
import com.madgag.github.GitHubCredentials
import com.madgag.scalagithub.GitHubCredentials
import lib.Implicits._
import org.joda.time.DateTime

import java.nio.file.Files
import scala.collection.convert.wrapAsScala._

object AuditDef {

  val parentWorkDir = Files.createTempDirectory("gu-who-working-dir")

  def safelyCreateFor(orgName: String, ghCreds: GitHubCredentials) = {
    val org = ghCreds.conn().getOrganization(orgName)
    AuditDef(org.getLogin, ghCreds)
  }
}

case class AuditDef(orgLogin: String, ghCreds: GitHubCredentials) {

  val workingDir = AuditDef.parentWorkDir / orgLogin.toLowerCase

  workingDir.mkdirs()

  lazy val (org, bot) = {
    val c = ghCreds.conn()

    val org = c.getOrganization(orgLogin)

    val bot = c.getMyself

    require(bot.isMemberOf(org))

    (org, bot)
  }

  def ensureSeemsLegit() = {
    require(bot.isMemberOf(org), s"Supplied bot account ${bot.atLogin} must be a member of ${org.atLogin}")

    val publicMembers = org.listPublicMembers.toStream

    lazy val publicOldMemberReqSummary =
      s"""
      |The organisation must have at least one public member whose GitHub account is over 3 months old.
      |If your account is over 3 months old, please go to ${org.membersAdminUrl}
      |and follow the 'make public' link against your user name. See also:
      |https://help.github.com/articles/publicizing-or-concealing-organization-membership
       """.stripMargin

    require(publicMembers.nonEmpty,
      s"Organisation ${org.atLogin} has no *public* members.$publicOldMemberReqSummary")
    require(publicMembers.exists(_.createdAt < DateTime.now - 3.months),
      s"Organisation ${org.atLogin} has ${publicMembers.size} public members, but none of those GitHub accounts are over 3 months old.$publicOldMemberReqSummary")
  }

}
