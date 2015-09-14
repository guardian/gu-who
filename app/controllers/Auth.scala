package controllers

import com.madgag.playgithub.auth.{AuthController, Client}
import lib.GithubAppConfig

object Auth extends AuthController {
  override val authClient: Client = GithubAppConfig.authClient
}
