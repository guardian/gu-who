package lib

import com.madgag.scalagithub.model.User
import com.madgag.scalagithub.{GitHub, GitHubCredentials}
import org.eclipse.jgit.transport.CredentialsProvider
import play.api.Logging

import java.nio.file.Path
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

case class Bot(
  workingDir: Path,
  github: GitHub,
  git: CredentialsProvider,
  user: User
)

object Bot extends Logging {
  def forAccessToken(accessToken: String)(implicit ec: ExecutionContext): Bot = {
    val workingDir = Path.of("/tmp", "bot", "working-dir")

    val credentials: GitHubCredentials =
      GitHubCredentials.forAccessKey(accessToken, workingDir).get

    val github: GitHub = new GitHub(credentials)
    val user: User = Await.result(github.getUser().map(_.result), 3.seconds)
    logger.info(s"Token gives GitHub user ${user.atLogin}")

    Bot(
      workingDir,
      github,
      credentials.git,
      user
    )
  }
}