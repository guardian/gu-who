package lib.actions

import com.madgag.playgithub
import com.madgag.playgithub.auth.AuthenticatedSessions.AccessToken
import com.madgag.playgithub.auth.AuthenticatedSessions.AccessToken.{FromBasicAuth, FromQueryString, FromSession}
import controllers.Auth

import scalax.file.ImplicitConversions._
import scalax.file.Path

object Actions {
  private val authScopes = Seq("repo", "write:org")

  implicit val accessTokenProvider = AccessToken.provider(FromBasicAuth, FromQueryString, FromSession)

  implicit val authClient = Auth.authClient

  val parentWorkDir = Path.fromString("/tmp") / "gu-who" / "working-dir"

  val GitHubAuthenticatedAction = playgithub.auth.Actions.gitHubAction(authScopes, parentWorkDir.toPath)

}