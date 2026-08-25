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

package controllers.orgemail

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.*
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Clear}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class OrganisationEmailAddressControllerSpec extends BaseUnitSpec {

  val validFormData: Map[String, String] = Map("value" -> testOrganisationEmailAddress)

  "OrganisationEmailAddressController.onPageLoad" should {

    "return 200 OK prefilled from the effective answers supplied by the retrieval action" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(organisationEmailAddress = Some(testOrganisationEmailAddress))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, organisationEmailAddressEndpoint)).value

        status(result)        shouldBe OK
        contentAsString(result) should include(testOrganisationEmailAddress)
      }
    }

    "return 200 OK with an empty form when there is nothing to prefill" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, organisationEmailAddressEndpoint)).value

        status(result)        shouldBe OK
        contentAsString(result) should not include testOrganisationEmailAddress
      }
    }
  }

  "OrganisationEmailAddressController.onSubmit" should {

    "send a verification code, save the answer and redirect when the form is valid" in {
      when(mockEmailVerificationConnector.sendCode(any())(any())).thenReturn(Future.successful(()))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, organisationEmailAddressEndpoint)
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER
        verify(mockEmailVerificationConnector).sendCode(eqTo(testOrganisationEmailAddress))(any())
        verify(mockUserAnswersRepository).set(any())
      }
    }

    "preserve an existing cached trading name when saving the answer" in {
      when(mockEmailVerificationConnector.sendCode(any())(any())).thenReturn(Future.successful(()))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(
        sessionAnswers = Some(UserAnswers(testSessionId, SessionUpdates(tradingName = Assign("Acme Savings Ltd"))))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, organisationEmailAddressEndpoint)
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe SessionUpdates(
          tradingName = Assign("Acme Savings Ltd"),
          organisationEmailAddress = Assign(testOrganisationEmailAddress),
          organisationEmailVerified = Clear
        )
      }
    }

    "return 400 BadRequest and not send a verification code when the form is invalid" in {
      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, organisationEmailAddressEndpoint)
            .withFormUrlEncodedBody("value" -> "not-an-email")
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        verify(mockEmailVerificationConnector, never).sendCode(any())(any())
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "return 500 InternalServerError and not save the answer when sending the verification code fails" in {
      when(mockEmailVerificationConnector.sendCode(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, organisationEmailAddressEndpoint)
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe INTERNAL_SERVER_ERROR
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
