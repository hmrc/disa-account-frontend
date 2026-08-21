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
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.disaaccountfrontend.models.emailverification.VerifyEmailCodeResult
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class EmailVerificationConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    val connector: EmailVerificationConnector =
      new EmailVerificationConnector(mockHttpClient, mockAppConfig)

    when(mockAppConfig.emailVerificationBaseUrl).thenReturn(emailVerificationBaseUrl)

    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
  }

  "EmailVerificationConnector.sendCode" should {

    "return Unit when the call succeeds" in new TestSetup {
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      connector.sendCode(testOrganisationEmailAddress).futureValue shouldBe (())
    }

    "include the email address in the request body" in new TestSetup {
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      connector.sendCode(testOrganisationEmailAddress).futureValue

      verify(mockRequestBuilder).withBody(any())(any(), any(), any())
    }

    "fail with an UpstreamErrorResponse when the backend returns a 4xx status" in new TestSetup {
      val upstreamBody = """{"code":"INVALID_REQUEST"}"""

      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, upstreamBody)))

      val thrown = connector.sendCode(testOrganisationEmailAddress).failed.futureValue

      thrown                                              shouldBe a[UpstreamErrorResponse]
      thrown.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe BAD_REQUEST
      thrown.asInstanceOf[UpstreamErrorResponse].reportAs   shouldBe INTERNAL_SERVER_ERROR
      thrown.getMessage                                     should include(upstreamBody)
    }

    "fail with an UpstreamErrorResponse when the backend returns a 5xx status" in new TestSetup {
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, "Service unavailable")))

      val thrown = connector.sendCode(testOrganisationEmailAddress).failed.futureValue

      thrown shouldBe a[UpstreamErrorResponse]
    }

    "propagate the failure when the call fails with an unexpected exception" in new TestSetup {
      val exception = new RuntimeException("Connection timeout")

      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.failed(exception))

      val thrown = connector.sendCode(testOrganisationEmailAddress).failed.futureValue

      thrown shouldBe exception
    }
  }

  "EmailVerificationConnector.verifyCode" should {

    "return Verified when the call succeeds" in new TestSetup {
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      connector.verifyCode(testOrganisationEmailAddress, "ABCDEF").futureValue shouldBe VerifyEmailCodeResult.Verified
    }

    "return InvalidCode when the backend returns a BAD_REQUEST" in new TestSetup {
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, """{"code":"CODE_NOT_VALID"}""")))

      connector
        .verifyCode(testOrganisationEmailAddress, "ABCDEF")
        .futureValue shouldBe VerifyEmailCodeResult.InvalidCode
    }

    "fail with an UpstreamErrorResponse when the backend returns a 5xx status" in new TestSetup {
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, "Service unavailable")))

      val thrown = connector.verifyCode(testOrganisationEmailAddress, "ABCDEF").failed.futureValue

      thrown                                              shouldBe a[UpstreamErrorResponse]
      thrown.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe SERVICE_UNAVAILABLE
      thrown.asInstanceOf[UpstreamErrorResponse].reportAs   shouldBe INTERNAL_SERVER_ERROR
    }

    "propagate the failure when the call fails with an unexpected exception" in new TestSetup {
      val exception = new RuntimeException("Connection timeout")

      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.failed(exception))

      val thrown = connector.verifyCode(testOrganisationEmailAddress, "ABCDEF").failed.futureValue

      thrown shouldBe exception
    }
  }
}
