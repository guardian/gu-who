package lib

import java.net.URLEncoder

object GithubAppConfig {

  import play.api.Play.current
  val config = play.api.Play.configuration

  val clientId = config.getString("securesocial.github.clientId").getOrElse("blah")
  val clientSecret = config.getString("securesocial.github.clientSecret").getOrElse("blah")
  val scope = "write:org,repo"
  val authUrl = "https://github.com/login/oauth/authorize"
  val accessTokenUrl = "https://github.com/login/oauth/access_token"

}

