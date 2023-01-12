package lib.actions

import com.madgag.playgithub
import com.madgag.playgithub.auth.AuthenticatedSessions.AccessToken
import com.madgag.playgithub.auth.AuthenticatedSessions.AccessToken.{FromBasicAuth, FromQueryString, FromSession, Provider}
import com.madgag.playgithub.auth.GHRequest
import com.madgag.scalagithub.model.RepoId
import controllers.{Auth, routes}
import lib.Bot
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilder, ActionFilter, AnyContent, BodyParser, Result}

import scala.concurrent.{ExecutionContext, Future}

class Actions(
  bot: Bot,
  bodyParser: BodyParser[AnyContent]
)(implicit
  authClient: com.madgag.playgithub.auth.Client,
  ec: ExecutionContext
) {
  private val authScopes = Seq("repo", "write:org")

  implicit val provider: Provider = AccessToken.provider(FromBasicAuth, FromQueryString)

  val GitHubAuthenticatedAction: ActionBuilder[GHRequest, AnyContent] =
    com.madgag.playgithub.auth.Actions.gitHubAction(authScopes, bot.workingDir, bodyParser)

  def repoAccessFilter(repoId: RepoId): ActionFilter[GHRequest] = new ActionFilter[GHRequest] {
    def executionContext = ec

    override protected def filter[A](req: GHRequest[A]): Future[Option[Result]] = {
      for {
        user <- req.userF
        userViewOfRepo <- req.gitHub.getRepo(repoId).trying
      } yield {
        println(s"******* ${user.atLogin} ${userViewOfRepo.map(r => r.full_name + " " + r.permissions)}")
        if (userViewOfRepo.isSuccess) None else Some(
          Redirect(routes.Application.index()).flashing("message" -> s"You can't see a ${repoId.fullName} repo")
        )
      }
    }
  }

  def repoAuthenticated(repoId: RepoId) = GitHubAuthenticatedAction andThen repoAccessFilter(repoId)
}