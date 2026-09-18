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

import uk.gov.hmrc.disaaccountfrontend.config.AppConfig

import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.temporal.ChronoUnit
import java.time.{Clock, LocalDate}
import java.util.Locale
import javax.inject.{Inject, Singleton}

@Singleton
class ReportingPeriodService @Inject() (appConfig: AppConfig, clock: Clock) {

  private def today: LocalDate = LocalDate.now(clock)

  private def monthName(date: LocalDate): String = date.getMonth.getDisplayName(TextStyle.FULL, Locale.UK)

  def reportingWindowMonth: String = monthName(today)

  def reportingPeriodMonth: String = monthName(today.minusMonths(1))

  def closingDate: LocalDate = today.withDayOfMonth(appConfig.reportingWindowClosingDay)

  def closingDateFormatted: String = closingDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.UK))

  def daysRemaining: Int = ChronoUnit.DAYS.between(today, closingDate).toInt
}
