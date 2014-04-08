package lib

import org.joda.time.format.PeriodFormatterBuilder
import com.github.nscala_time.time.Imports._
import org.kohsuke.github.GHIssue


object TerminationSchedule {

  val Relaxed = TerminationSchedule(4.weeks + 1.day + 6.hours, 1.week)

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
  lazy val warnedLabel: String = "Warned"+TerminationSchedule.LabelPeriodFormatter.print(finalWarningPeriod)

  def terminationDateFor(issue: GHIssue) = issue.getCreatedAt.getTime.toDateTime + tolerancePeriod
}
