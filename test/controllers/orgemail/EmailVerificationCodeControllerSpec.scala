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
import uk.gov.hmrc.disaaccountfrontend.models.emailverification.VerifyEmailCodeResult
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class EmailVerificationCodeControllerSpec extends BaseUnitSpec {

  val effectiveAnswersWithEmail: Answers = Answers(organisationEmailAddress = Some(testOrganisationEmailAddress))

  "EmailVerificationCodeController.onPageLoad" should {

    "return 200 OK when an organisation email address is present" in {
      val application = applicationBuilder(effectiveAnswers = effectiveAnswersWithEmail).build()

      running(application) {
        val result = route(application, FakeRequest(GET, emailVerificationCodeEndpoint)).value
        val html   = contentAsString(result)

        status(result) shouldBe OK
        html             should include(testOrganisationEmailAddress)
      }
    }

    "redirect to the organisation email address page when no email address has been entered" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, emailVerificationCodeEndpoint)).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(organisationEmailAddressEndpoint)
      }
    }
  }

  "EmailVerificationCodeController.onSubmit" should {

    "redirect and save the email as verified when the code is verified" in {
      when(mockEmailVerificationConnector.verifyCode(any(), any())(any()))
        .thenReturn(Future.successful(VerifyEmailCodeResult.Verified))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(effectiveAnswers = effectiveAnswersWithEmail).build()

      running(application) {
        val request = FakeRequest(POST, emailVerificationCodeEndpoint)
          .withFormUrlEncodedBody("value" -> "ABCDEF")
          .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER
        verify(mockEmailVerificationConnector).verifyCode(eqTo(testOrganisationEmailAddress), eqTo("ABCDEF"))(any())

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe SessionUpdates(organisationEmailVerified = Assign(true))
      }
    }

    "return 400 BadRequest with an inline error when the code is invalid" in {
      when(mockEmailVerificationConnector.verifyCode(any(), any())(any()))
        .thenReturn(Future.successful(VerifyEmailCodeResult.InvalidCode))

      val application = applicationBuilder(effectiveAnswers = effectiveAnswersWithEmail).build()

      running(application) {
        val request = FakeRequest(POST, emailVerificationCodeEndpoint)
          .withFormUrlEncodedBody("value" -> "ABCDEF")
          .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)        shouldBe BAD_REQUEST
        contentAsString(result) should include(messages("emailVerificationCode.error.invalid")(application))
      }
    }

    "return 400 BadRequest and not call the connector when the form is invalid" in {
      val application = applicationBuilder(effectiveAnswers = effectiveAnswersWithEmail).build()

      running(application) {
        val request = FakeRequest(POST, emailVerificationCodeEndpoint)
          .withFormUrlEncodedBody("value" -> "12345")
          .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        verify(mockEmailVerificationConnector, never).verifyCode(any(), any())(any())
      }
    }

    "return 500 InternalServerError when verifying the code fails" in {
      when(mockEmailVerificationConnector.verifyCode(any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(effectiveAnswers = effectiveAnswersWithEmail).build()

      running(application) {
        val request = FakeRequest(POST, emailVerificationCodeEndpoint)
          .withFormUrlEncodedBody("value" -> "ABCDEF")
          .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to the organisation email address page when no email address has been entered" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, emailVerificationCodeEndpoint)
          .withFormUrlEncodedBody("value" -> "ABCDEF")
          .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(organisationEmailAddressEndpoint)
        verify(mockEmailVerificationConnector, never).verifyCode(any(), any())(any())
      }
    }
  }

  "EmailVerificationCodeController.requestNewCode" should {

    "send a new code, clear any prior verification and redirect back to the entry page" in {
      when(mockEmailVerificationConnector.sendCode(any())(any())).thenReturn(Future.successful(()))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(effectiveAnswers = effectiveAnswersWithEmail).build()

      running(application) {
        val request = FakeRequest(POST, requestNewCodeEndpoint).withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(emailVerificationCodeEndpoint)
        verify(mockEmailVerificationConnector).sendCode(eqTo(testOrganisationEmailAddress))(any())

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe SessionUpdates(organisationEmailVerified = Clear)
      }
    }

    "return 500 InternalServerError when sending a new code fails" in {
      when(mockEmailVerificationConnector.sendCode(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(effectiveAnswers = effectiveAnswersWithEmail).build()

      running(application) {
        val request = FakeRequest(POST, requestNewCodeEndpoint).withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
