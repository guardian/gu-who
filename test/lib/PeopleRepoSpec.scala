package lib

import com.madgag.git.test._
import org.scalatestplus.play.PlaySpec

import scalax.file.ImplicitConversions._
import scalax.file.Path

class PeopleRepoSpec extends PlaySpec {

  "Retrieving sponsored users" should {
    "extract sponsored user logins after cloning a remote repo" in {

      implicit val demoRemoteRepo = unpackRepo("/demo-people-repos/minimal-people.git.zip")

      val logins = PeopleRepo.getSponsoredUserLogins(Path.createTempDirectory(), demoRemoteRepo.getDirectory.getAbsolutePath)

      logins mustEqual Set("philwills", "tackley", "shufgy", "rtyley", "lindseydew")
    }

    "return an empty set, when no users.txt found" in {

      implicit val demoRemoteRepo = unpackRepo("/demo-people-repos/people.no-users-txt.git.zip")

      val logins = PeopleRepo.getSponsoredUserLogins(Path.createTempDirectory(), demoRemoteRepo.getDirectory.getAbsolutePath)

      logins mustBe empty
    }
  }



}
