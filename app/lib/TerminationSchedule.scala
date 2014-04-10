/*
 * Copyright 2014 The Guardian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
