package lib

import com.squareup.okhttp.{OkHttpClient, HttpResponseCache}
import java.io.File
import org.kohsuke.github.GitHub
import java.net.URL

object OkGitHub {
  val cache: HttpResponseCache = new HttpResponseCache(new File("http-cache"), Integer.MAX_VALUE)

  lazy val okHttpClient = {
    val client = new OkHttpClient
    client.setOkResponseCache(cache)
    client
  }

  def conn(githubApiKey: String) = new GitHub(null, githubApiKey, null) {
    override def open(url: URL) = okHttpClient.open(url)
  }
}
