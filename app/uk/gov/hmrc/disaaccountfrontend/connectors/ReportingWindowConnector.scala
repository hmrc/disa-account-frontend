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

package uk.gov.hmrc.disaaccountfrontend.connectors

import play.api.Logging
import uk.gov.hmrc.disaaccountfrontend.config.AppConfig
import uk.gov.hmrc.disaaccountfrontend.models.reportingwindow.ReportingWindowStatus
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingWindowConnector @Inject() (
  http: HttpClientV2,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def getReportingWindowStatus(zref: String)(implicit hc: HeaderCarrier): Future[ReportingWindowStatus] = {
    val url = s"${appConfig.disaReturnsSubmissionBaseUrl}/disa-returns-submission/reporting-window/status/$zref"
    http
      .get(url"$url")
      .setHeader("Authorization" -> appConfig.internalAuthToken)
      .execute[Either[UpstreamErrorResponse, ReportingWindowStatus]]
      .flatMap {
        case Right(status) => Future.successful(status)
        case Left(error)   => Future.failed(error)
      }
  }
}
