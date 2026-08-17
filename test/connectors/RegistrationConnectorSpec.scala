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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.disaaccountfrontend.models.registration.RegistrationDetails
import uk.gov.hmrc.http.{StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class RegistrationConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    val connector: RegistrationConnector =
      new RegistrationConnector(mockHttpClient, mockAppConfig, retryConfig, actorSystem)

    val testUrl: String = "http://localhost:12105"
    when(mockAppConfig.disaAccountBaseUrl).thenReturn(testUrl)

    when(mockHttpClient.get(url"$testUrl/disa-account/registration/$testZref"))
      .thenReturn(mockRequestBuilder)
  }

  "RegistrationConnector.getRegistrationDetails" should {

    "return Some(RegistrationDetails) when the call succeeds" in new TestSetup {
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, RegistrationDetails]](any(), any()))
        .thenReturn(Future.successful(Right(testRegistrationDetails)))

      val result: Option[RegistrationDetails] = connector.getRegistrationDetails(testZref).futureValue

      result shouldBe Some(testRegistrationDetails)
    }

    "return None when the backend returns a 404" in new TestSetup {
      val notFound: UpstreamErrorResponse = UpstreamErrorResponse("Not found", 404, 404, Map.empty)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, RegistrationDetails]](any(), any()))
        .thenReturn(Future.successful(Left(notFound)))

      val result: Option[RegistrationDetails] = connector.getRegistrationDetails(testZref).futureValue

      result shouldBe None
    }

    "propagate the failure when the backend returns an unexpected error, so the journey is not entered" in new TestSetup {
      val serverError: UpstreamErrorResponse = UpstreamErrorResponse("Boom", 500, 500, Map.empty)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, RegistrationDetails]](any(), any()))
        .thenReturn(Future.successful(Left(serverError)))

      val thrown = connector.getRegistrationDetails(testZref).failed.futureValue

      thrown shouldBe serverError
    }

    "propagate the failure when the call fails with an unexpected exception, so the journey is not entered" in new TestSetup {
      val exception = new RuntimeException("Connection timeout")

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, RegistrationDetails]](any(), any()))
        .thenReturn(Future.failed(exception))

      val thrown = connector.getRegistrationDetails(testZref).failed.futureValue

      thrown shouldBe exception
    }
  }
}
