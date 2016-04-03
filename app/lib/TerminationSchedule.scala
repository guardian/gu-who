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

import java.time.ZonedDateTime
import java.time.temporal.{ChronoUnit, Temporal, TemporalUnit, TemporalAmount}
import java.util

import org.joda.time.DurationFieldType
import org.joda.time.format.PeriodFormatterBuilder
import com.github.nscala_time.time.Imports._
import com.madgag.time.Implicits._
import com.madgag.scalagithub.model.Issue
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

object Foo {
  implicit def jodaDurationFieldType2javaTemporalUnit(jdft: DurationFieldType) =
    ChronoUnit.valueOf(jdft.getName.capitalize)

  implicit def jodaPeriod2javaTemporalAmount(jodaPeriod: org.joda.time.Period) = {
    new TemporalAmount {
      override def addTo(temporal: Temporal) = {
        temporal match {
          case zdt: ZonedDateTime => for {
            fieldType <- jodaPeriod.getFieldTypes
          } {
            zdt.plus(jodaPeriod.get(fieldType), fieldType)
          }
        }
        temporal
      }

      override def get(unit: TemporalUnit): Long = ???

      override def subtractFrom(temporal: Temporal): Temporal = ???

      override def getUnits: util.List[TemporalUnit] = ???

    }
  }
}

case class TerminationSchedule(tolerancePeriod: Period, finalWarningPeriod: Period) {
  lazy val warnedLabel: String = "Warned"+LabelPeriodFormatter.print(finalWarningPeriod)

  def terminationDateFor(issue: Issue) =
    Seq(EarliestTerminationDate, tolerancePeriod.from(issue.created_at.get)).max

}
