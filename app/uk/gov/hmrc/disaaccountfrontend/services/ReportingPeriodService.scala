/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.disaaccountfrontend.services

import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, LocalDate}
import java.util.Locale
import javax.inject.{Inject, Singleton}

@Singleton
class ReportingPeriodService @Inject() (clock: Clock) {

  private def monthName(date: LocalDate): String = date.getMonth.getDisplayName(TextStyle.FULL, Locale.UK)

  def reportingWindowMonth(reportingWindowEnd: Instant): String = monthName(closingDate(reportingWindowEnd))

  def reportingPeriodMonth(reportingWindowEnd: Instant): String =
    monthName(closingDate(reportingWindowEnd).minusMonths(1))

  def closingDate(reportingWindowEnd: Instant): LocalDate = LocalDate.ofInstant(reportingWindowEnd, clock.getZone)

  def closingDateFormatted(reportingWindowEnd: Instant): String =
    closingDate(reportingWindowEnd).format(DateTimeFormatter.ofPattern("d MMMM", Locale.UK))

  def daysRemaining(resolvedAt: Instant, reportingWindowEnd: Instant): Int =
    ChronoUnit.DAYS.between(LocalDate.ofInstant(resolvedAt, clock.getZone), closingDate(reportingWindowEnd)).toInt
}
