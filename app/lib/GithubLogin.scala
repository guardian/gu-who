package lib

import java.net.URLEncoder

trait GithubLogin {
    abstract val baseUrl: String
    abstract val queryStringParams: Map[String, String]
    val clientId = "312fb1f543eff4c2a69c"
    val clientSecret = "26a2a723f0c4482ff4c562c29bdd973f483768fd"
    val scope = "read:org,write:org"
    val state = "ASUPERRANDOMSTRING"

    lazy val queryString: String = {
      queryStringParams.map {
        case (k, v) => k + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
    }

    lazy val uri = baseUrl + "?" + queryString
}

object GithubAuthUri extends GithubLogin {
  val queryStringParams = Map("client_id" -> clientId, "state" -> state, "scope" -> scope)
  val baseUrl = "https://github.com/login/oauth/authorize"
}

class GithubAccessTokenUri(code: String, state: String) extends GithubLogin {
  val queryStringParams = Map("code" -> code, "client_id" -> clientId, "client_secret" -> clientSecret)
  val baseUrl = "https://github.com/login/oauth/access_token"
}