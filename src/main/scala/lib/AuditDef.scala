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

import com.madgag.scalagithub.model.{Org, RepoId, User}
import com.madgag.scalagithub.{GitHub, GitHubCredentials}
import lib.model.GuWhoOrg

import java.nio.file.{Files, Path}
import scala.concurrent.{ExecutionContext, Future}

object AuditDef {

  val parentWorkDir = Files.createTempDirectory("gu-who-working-dir")

  def safelyCreateFor(peopleRepoId: RepoId, ghCreds: GitHubCredentials)(
    implicit ec: ExecutionContext
  ): Future[AuditDef] = {
    val gitHub = new GitHub(ghCreds)
    for {
      org <- gitHub.getOrg(peopleRepoId.owner)
      peopleRepo <- gitHub.getRepo(peopleRepoId)
      bot <- gitHub.getUser()
      botIsMemberOfOrg <- gitHub.checkMembership(org.login, bot.login)
    } yield {
      require(botIsMemberOfOrg)
      AuditDef(GuWhoOrg(org.result, peopleRepo), bot.result, ghCreds)
    }
  }
}

case class AuditDef(guWhoOrg: GuWhoOrg, bot: User, ghCreds: GitHubCredentials) {

  val workingDir: Path = Files.createTempDirectory(s"gu-who-${guWhoOrg.org.login.toLowerCase}")

  Files.createDirectories(workingDir)

}
