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

package connectors

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.disaaccountfrontend.connectors.ReportingWindowConnector
import uk.gov.hmrc.disaaccountfrontend.models.reportingwindow.ReportingWindowStatus
import uk.gov.hmrc.http.{StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec

import java.time.Instant
import scala.concurrent.Future

class ReportingWindowConnectorSpec extends BaseUnitSpec {

  private val testInternalAuthToken = "valid-internal-auth-token-disa-account-frontend"
  private val testWindowStart       = Instant.parse("2026-07-06T00:00:00Z")
  private val testWindowEnd         = Instant.parse("2026-07-19T23:59:59Z")
  private val testResolvedAt        = Instant.parse("2026-07-08T09:00:00Z")

  trait TestSetup {
    val connector: ReportingWindowConnector =
      new ReportingWindowConnector(mockHttpClient, mockAppConfig, retryConfig, actorSystem)

    when(mockAppConfig.disaReturnsSubmissionBaseUrl).thenReturn(disaReturnsSubmissionBaseUrl)
    when(mockAppConfig.internalAuthToken).thenReturn(testInternalAuthToken)

    when(mockHttpClient.get(url"${reportingWindowStatusEndpoint(testZref)}"))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
  }

  "ReportingWindowConnector.getReportingWindowStatus" should {

    "return the status when the window is open" in new TestSetup {
      val status: ReportingWindowStatus =
        ReportingWindowStatus(reportingWindowOpen = true, testWindowStart, testWindowEnd, testResolvedAt)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, ReportingWindowStatus]](any(), any()))
        .thenReturn(Future.successful(Right(status)))

      val result: ReportingWindowStatus = connector.getReportingWindowStatus(testZref).futureValue

      result shouldBe status
      verify(mockRequestBuilder).setHeader("Authorization" -> testInternalAuthToken)
    }

    "return the status when the window is closed" in new TestSetup {
      val status: ReportingWindowStatus =
        ReportingWindowStatus(reportingWindowOpen = false, testWindowStart, testWindowEnd, testResolvedAt)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, ReportingWindowStatus]](any(), any()))
        .thenReturn(Future.successful(Right(status)))

      val result: ReportingWindowStatus = connector.getReportingWindowStatus(testZref).futureValue

      result shouldBe status
    }

    "propagate the failure when the backend returns an unexpected error" in new TestSetup {
      val serverError: UpstreamErrorResponse =
        UpstreamErrorResponse("Boom", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, Map.empty)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, ReportingWindowStatus]](any(), any()))
        .thenReturn(Future.successful(Left(serverError)))

      val thrown = connector.getReportingWindowStatus(testZref).failed.futureValue

      thrown shouldBe serverError
    }

    "propagate the failure when the call fails with an unexpected exception" in new TestSetup {
      val exception = new RuntimeException("Connection timeout")

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, ReportingWindowStatus]](any(), any()))
        .thenReturn(Future.failed(exception))

      val thrown = connector.getReportingWindowStatus(testZref).failed.futureValue

      thrown shouldBe exception
    }
  }
}
