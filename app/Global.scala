import controllers.{AddTeamMembers, GitHubPoller}
import play.api.libs.concurrent.Akka
import akka.actor.Props
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.{Logger, Play, GlobalSettings}
import Play.current

object Global extends GlobalSettings {
  override def onStart(app: play.api.Application)  {
    Logger.info("starting my app")
    val githubPoller = Akka.system.actorOf(Props[GitHubPoller])
    Akka.system.scheduler.schedule(
      initialDelay = 0.seconds,
      interval = 60.minutes,
      githubPoller,
      AddTeamMembers
    )
  }

}
