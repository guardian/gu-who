package lib

import org.kohsuke.github.GHUser


case class OrgSnapshot(sponsoredUserLogins: Set[String], twoFactorAuthDisabledUserLogins: Set[GHUser])
