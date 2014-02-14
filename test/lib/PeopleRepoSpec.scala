package lib

import org.specs2.mutable.Specification
import com.madgag.git.test._
import scalax.file.ImplicitConversions._
import scalax.file.Path

class PeopleRepoSpec extends Specification {

  "Retrieving sponsored users" should {
    "extract sponsored user logins after cloning a remote repo" in {

      implicit val demoRemoteRepo = unpackRepo("/demo-people-repos/minimal-people.git.zip")

      val logins = PeopleRepo.getSponsoredUserLogins(Path.createTempDirectory(), demoRemoteRepo.getDirectory.getAbsolutePath)

      logins mustEqual(Set("philwills", "tackley", "shufgy", "rtyley", "lindseydew"))
    }
  }

}
