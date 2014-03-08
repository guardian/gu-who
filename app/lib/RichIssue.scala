package lib

import org.kohsuke.github.GHIssue

object Implicits {
  implicit class RichIssue(issue: GHIssue) {
    lazy val assignee = Option(issue.getAssignee)
  }
}
