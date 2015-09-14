package lib

import com.madgag.heroku.DynoMetadata.gitCommitIdFromHerokuFile

object BuildCommit {

  lazy val gitCommitId = gitCommitIdFromHerokuFile.getOrElse(app.BuildInfo.gitCommitId)

}
