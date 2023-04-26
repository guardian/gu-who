package lib

import com.madgag.git.test._
import org.scalatestplus.play.PlaySpec


class PeopleRepoSpec extends AnyFlatSpec {

  val githubToken = sys.env("PROUT_GITHUB_ACCESS_TOKEN")

  val githubCredentials =
    GitHubCredentials.forAccessKey(githubToken, Files.createTempDirectory("tmpDirPrefix")).get

  "Retrieving sponsored users" should {
    "extract sponsored user logins after cloning a remote repo" in {

      implicit val demoRemoteRepo = unpackRepo("/demo-people-repos/minimal-people.git.zip")

      val logins = PeopleRepo.getSponsoredUserLogins(Path.createTempDirectory(), demoRemoteRepo.getDirectory.getAbsolutePath, defaultBranch = "master")
      logins mustEqual Set("philwills", "tackley", "shufgy", "rtyley", "lindseydew")
    }

    "return an empty set, when no users.txt found" in {

      implicit val demoRemoteRepo = unpackRepo("/demo-people-repos/people.no-users-txt.git.zip")

      val logins = PeopleRepo.getSponsoredUserLogins(Path.createTempDirectory(), demoRemoteRepo.getDirectory.getAbsolutePath, defaultBranch = "master")
      logins mustBe empty
    }

    "extract sponsored user logins after cloning a remote repo with main default branch" in {

      implicit val demoRemoteRepo = unpackRepo("/demo-people-repos/minimal-people-main.git.zip")
      val logins = PeopleRepo.getSponsoredUserLogins(Path.createTempDirectory(), demoRemoteRepo.getDirectory.getAbsolutePath, defaultBranch = "main")
      logins mustEqual Set("rtyley", "markjamesbutler")
    }

    "return an empty set, with non existent default branch" in {

      implicit val demoRemoteRepo = unpackRepo("/demo-people-repos/minimal-people.git.zip")

      val logins = PeopleRepo.getSponsoredUserLogins(Path.createTempDirectory(), demoRemoteRepo.getDirectory.getAbsolutePath, defaultBranch = "main")
      logins mustBe empty
    }
  }



}
