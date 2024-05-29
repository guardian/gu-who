import com.madgag.playgithub.testkit.{RepoLifecycle, TestRepoCreation}
import org.eclipse.jgit.lib.ObjectId.zeroId
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, Inside}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class FunctionalSpec extends AnyFlatSpec with Inside with BeforeAndAfterAll with TestRepoCreation {

  val testRepoNamePrefix: String = "gu-who-test"

  val repoLifecycle: RepoLifecycle = OrgRepoLifecycle()

  override def beforeAll(): Unit = {
    deleteTestRepos()
  }

  "gu:who" should "create issues for users that have problems" in {
    val repo = createTestRepo("/feature-branches.top-level-config.git.zip")
  }

}