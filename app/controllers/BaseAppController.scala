package controllers

import com.madgag.playgithub.auth.GHRequest
import com.madgag.scalagithub.model.RepoId
import lib.actions.Actions
import play.api.Logging
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext

case class ControllerAppComponents(
  actions: Actions,
  actionBuilder: DefaultActionBuilder,
  parsers: PlayBodyParsers,
  messagesApi: MessagesApi,
  langs: Langs,
  fileMimeTypes: FileMimeTypes,
  executionContext: scala.concurrent.ExecutionContext
) extends ControllerComponents

trait BaseAppController extends BaseController with Logging {

  val controllerAppComponents: ControllerAppComponents

  override val controllerComponents = controllerAppComponents

  implicit val ec: ExecutionContext = controllerAppComponents.executionContext // Controversial? https://www.playframework.com/documentation/2.6.x/ThreadPools

//  def repoAuthenticated(repoId: RepoId): ActionBuilder[GHRequest, AnyContent] =
//    controllerAppComponents.actions.repoAuthenticated(repoId)

}

abstract class AbstractAppController(
  val controllerAppComponents: ControllerAppComponents
) extends BaseAppController