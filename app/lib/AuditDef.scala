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

import com.madgag.scalagithub.GitHub
import GitHub._
import com.madgag.scalagithub.model.{User, Org}
import lib.Implicits._

import scala.concurrent.Future
import scalax.file.ImplicitConversions._
import scalax.file.Path
import scala.concurrent.ExecutionContext.Implicits.global

object AuditDef {

  val parentWorkDir = Path.fromString("/tmp") / "gu-who" / "working-dir"

  def safelyCreateFor(orgName: String)(implicit github: GitHub) = {
    for {
      org <- github.getOrg(orgName)
      bot <- github.getUser()
      botIsMemberOfOrg <- org.members.check(bot.login)
      publicMembers: Seq[User] <- org.publicMembers.list().all()
      publicMembersBlah <- Future.find(publicMembers.map { u:User => u.reFetch() })
    } yield {
      def ensureSeemsLegit() = {
        require(botIsMemberOfOrg, s"Supplied bot account ${bot.atLogin} must be a member of ${org.atLogin}")

        lazy val publicOldMemberReqSummary =
          s"""
             |The organisation must have at least one public member whose GitHub account is over 3 months old.
             |If your account is over 3 months old, please go to ${org.membersAdminUrl}

             |and follow the 'make public' link against your user name. See also:
             |https://help.github.com/articles/publicizing-or-concealing-organization-membership
       """

            .
              stripMargin

        require(
          publicMembers.nonEmpty,
          s"Organisation ${org.atLogin} has no *public* members.$publicOldMemberReqSummary")
        //      require(publicMembers.exists(_.created_at.exists(_ < DateTime.now - 3.months)),
        //        s"Organisation ${org.atLogin} has ${publicMembers.size}" +


        //           "public members, but none of those GitHub accounts are over 3 months old.$publicOldMemberReqSummary")
        //    }
      }


      AuditDef(bot, org, github)
    }
  }
}

case class AuditDef(bot: User, org: Org, github: GitHub) {

  val workingDir = AuditDef.parentWorkDir / org.login.toLowerCase

  workingDir.mkdirs()

  // require(bot.isMemberOf(org))


}
