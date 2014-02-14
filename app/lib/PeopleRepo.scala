package lib

import org.eclipse.jgit.api.{GitCommand, TransportCommand, Git}
import org.eclipse.jgit.transport.{CredentialsProvider, UsernamePasswordCredentialsProvider}
import com.madgag.git._
import scalax.io.Resource
import java.io.File
import org.eclipse.jgit.lib.Repository
import scalax.file.ImplicitConversions._
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import play.api.Logger

object PeopleRepo {

  val UsernameRegex = """^([\p{Alnum}-]{2,}+)""".r

  def getSponsoredUserLogins(dataDirectory: File, uri: String, credentials: Option[CredentialsProvider] = None): Set[String] = {

    def invoke[C <: GitCommand[_], R](command: TransportCommand[C, R]): R = {
      credentials.foreach(command.setCredentialsProvider)
      command.call()
    }

    def getUpToDateRepo(): Repository = {
      val gitdir = dataDirectory / "people.git"

      if (gitdir.exists) {
        Logger.info("Updating Git repo with fetch...")
        val repo = FileRepositoryBuilder.create(gitdir)
        invoke(repo.git.fetch())
        repo
      } else {
        Logger.info("Cloning new Git repo...")
        invoke(Git.cloneRepository().setBare(true).setDirectory(gitdir).setURI(uri)).getRepository
      }
    }

    val repo = getUpToDateRepo()

    implicit val reader = repo.newObjectReader()

    val latestUserFileGitId = repo.resolve("master^{tree}:users.txt")

    val lines = Resource.fromInputStream(latestUserFileGitId.open.openStream()).lines()

    lines.collect { case UsernameRegex(username) => username }.toSet
  }

}