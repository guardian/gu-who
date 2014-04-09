package lib

import org.joda.time.format.PeriodFormatterBuilder
import com.github.nscala_time.time.Imports._
import org.kohsuke.github.GHIssue
import TerminationSchedule._

object TerminationSchedule {

  val EarliestTerminationDate = new DateTime(2014, 4, 9, 9, 0, DateTimeZone.UTC)

  val Relaxed = TerminationSchedule(4.weeks, 1.week)

  val LabelPeriodFormatter = new PeriodFormatterBuilder()
    .appendYears().appendSuffix("Y")
    .appendMonths().appendSuffix("M")
    .appendWeeks().appendSuffix("W")
    .appendDays().appendSuffix("D")
    .appendHours().appendSuffix("H")
    .appendMinutes().appendSuffix("M")
    .appendSecondsWithOptionalMillis().appendSuffix("S")
    .toFormatter()
}

case class TerminationSchedule(tolerancePeriod: Period, finalWarningPeriod: Period) {
  lazy val warnedLabel: String = "Warned"+LabelPeriodFormatter.print(finalWarningPeriod)

  def terminationDateFor(issue: GHIssue) =
    Seq(EarliestTerminationDate, issue.getCreatedAt.getTime.toDateTime + tolerancePeriod).max

}
